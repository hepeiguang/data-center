package org.apache.dolphinscheduler.meter.metrics;

import org.apache.dolphinscheduler.common.utils.OSUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ContainerMetrics {
    // CPU 监控相关状态
    private long lastCpuTime = 0;
    private long lastTimestamp = 0;
    private long lastProcessCpuTime = 0;
    private long lastProcessTimestamp = 0;

    // 内存监控相关状态
    private double lastMemoryCheckTime = 0;
    private double lastMemoryUsage = 0;

    // 获取系统CPU使用率（容器感知）
    public double getSystemCpuUsage() {
        try {
            CpuContext context = getCpuContext();
            if (context.unlimited) {
                return calculateUnlimitedCpuUsage();
            }
            return calculateUsage(context.cpuTime, context.quota, context.period);
        } catch (Exception e) {
            return -1;
        }
    }

    private double calculateUsage(long cpuTime, double quota, double period) {
        long currentTime = System.nanoTime();

        // 初次调用直接返回0
        if (lastCpuTime == 0) {
            lastCpuTime = cpuTime;
            lastTimestamp = currentTime;
            return 0;
        }

        // 计算CPU使用增量
        long cpuDelta = cpuTime - lastCpuTime;
        long timeDelta = currentTime - lastTimestamp;

        // 计算可用CPU核心数
        double availableCores = quota / period;

        // CPU使用率 = (CPU时间增量 / 时间增量) / 可用核心数 * 100%
        double usage = (cpuDelta * 1.0) / (timeDelta * availableCores);

        // 更新状态
        lastCpuTime = cpuTime;
        lastTimestamp = currentTime;

        // 确保返回值在合理范围内
        return Math.min(100.0, Math.max(0.0, usage));
    }

    // 获取当前进程CPU使用率
    public double getProcessCpuUsage() {
        try {
            long processCpuTime = getCurrentProcessCpuTime();
            long currentTime = System.nanoTime();

            if (lastProcessCpuTime == 0) {
                lastProcessCpuTime = processCpuTime;
                lastProcessTimestamp = currentTime;
                return 0.0;
            }

            CpuContext context = getCpuContext();
            double usage = (processCpuTime - lastProcessCpuTime) * 100.0 /
                    (currentTime - lastProcessTimestamp);

            if (!context.unlimited) {
                usage /= (context.quota / context.period);
            }

            lastProcessCpuTime = processCpuTime;
            lastProcessTimestamp = currentTime;

            return Math.min(100.0, Math.max(0.0, usage));
        } catch (Exception e) {
            return -1;
        }
    }

    // 获取容器内存使用率
    public double getMemoryUsage() {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMemoryCheckTime < 1000 && lastMemoryUsage >= 0) {
                return lastMemoryUsage;
            }

            MemoryContext context = getMemoryContext();
            if (context.unlimited) {
                lastMemoryUsage = -1;
                return -1;
            }

            double usage = (context.used * 100.0) / context.limit;
            lastMemoryUsage = Math.min(100.0, Math.max(0.0, usage));
            lastMemoryCheckTime = currentTime;

            return lastMemoryUsage;
        } catch (Exception e) {
            return -1;
        }
    }

    // 内部数据结构
    private static class CpuContext {
        final long cpuTime;
        final double quota;
        final double period;
        final boolean unlimited;

        CpuContext(long cpuTime, double quota, double period, boolean unlimited) {
            this.cpuTime = cpuTime;
            this.quota = quota;
            this.period = period;
            this.unlimited = unlimited;
        }
    }

    private static class MemoryContext {
        final long used;
        final long limit;
        final boolean unlimited;

        MemoryContext(long used, long limit, boolean unlimited) {
            this.used = used;
            this.limit = limit;
            this.unlimited = unlimited;
        }
    }

    // 核心实现方法
    private CpuContext getCpuContext() throws Exception {
        // cgroups v2 路径
        Path cpuStatPath = Paths.get("/sys/fs/cgroup/cpu.stat");
        if (Files.exists(cpuStatPath)) {
            Map<String, Long> stats = parseCpuStat(cpuStatPath);
            String[] maxParts = readFile(cpuStatPath.getParent().resolve("cpu.max"))
                    .split(" ");

            boolean unlimited = "max".equals(maxParts[0]);
            double quota = unlimited ?
                    Runtime.getRuntime().availableProcessors() : Double.parseDouble(maxParts[0]);
            double period = Double.parseDouble(maxParts[1]);

            return new CpuContext(
                    stats.get("usage_usec") * 1000L,
                    quota,
                    period,
                    unlimited
            );
        }

        // cgroups v1 路径
        Path cpuAcctPath = Paths.get("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage");
        if (Files.exists(cpuAcctPath)) {
            long quota = Long.parseLong(readFile(
                    Paths.get("/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us")));
            long period = Long.parseLong(readFile(
                    Paths.get("/sys/fs/cgroup/cpu,cpuacct/cpu.cfs_period_us")));

            boolean unlimited = quota == -1;
            if (unlimited) {
                quota = Runtime.getRuntime().availableProcessors() * period;
            }

            return new CpuContext(
                    Long.parseLong(readFile(cpuAcctPath)),
                    quota,
                    period,
                    unlimited
            );
        }

        // 默认返回宿主机状态
        return new CpuContext(
                getTotalCpuTime(),
                Runtime.getRuntime().availableProcessors(),
                1.0,
                true
        );
    }

    private MemoryContext getMemoryContext() throws Exception {
        // cgroups v2 路径
        Path memCurrentPath = Paths.get("/sys/fs/cgroup/memory.current");
        if (Files.exists(memCurrentPath)) {
            String max = readFile(Paths.get("/sys/fs/cgroup/memory.max"));
            boolean unlimited = "max".equals(max.trim());

            return new MemoryContext(
                    Long.parseLong(readFile(memCurrentPath)),
                    unlimited ? Long.MAX_VALUE : Long.parseLong(max),
                    unlimited
            );
        }

        // cgroups v1 路径
        Path memUsagePath = Paths.get("/sys/fs/cgroup/memory/memory.usage_in_bytes");
        if (Files.exists(memUsagePath)) {
            String limit = readFile(Paths.get("/sys/fs/cgroup/memory/memory.limit_in_bytes"));
            boolean unlimited = limit.trim().equals("9223372036854771712");

            return new MemoryContext(
                    Long.parseLong(readFile(memUsagePath)),
                    unlimited ? Long.MAX_VALUE : Long.parseLong(limit),
                    unlimited
            );
        }

        // 默认返回宿主机状态
        return new MemoryContext(
                OSUtils.getTotalSystemMemory() - OSUtils.getSystemAvailableMemoryUsed(),
                OSUtils.getTotalSystemMemory(),
                true
        );
    }

    // 辅助方法
    private Map<String, Long> parseCpuStat(Path path) throws Exception {
        Map<String, Long> stats = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            br.lines().forEach(line -> {
                String[] parts = line.split(" ");
                stats.put(parts[0], Long.parseLong(parts[1]));
            });
        }
        return stats;
    }

    private String readFile(Path path) throws Exception {
        return new String(Files.readAllBytes(path)).trim();
    }

    private long getCurrentProcessCpuTime() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/self/stat"))) {
            String[] parts = br.readLine().split("\\s+");
            return (Long.parseLong(parts[13]) + // utime
                    Long.parseLong(parts[14]) + // stime
                    Long.parseLong(parts[15]) + // cutime
                    Long.parseLong(parts[16])) * // cstime
                    (1_000_000_000L / 100); // 转换为纳秒
        }
    }

    private double calculateUnlimitedCpuUsage() throws Exception {
        long cpuTime = getTotalCpuTime();
        long currentTime = System.nanoTime();

        if (lastCpuTime == 0) {
            lastCpuTime = cpuTime;
            lastTimestamp = currentTime;
            return 0;
        }

        double usage = (cpuTime - lastCpuTime) * 100.0 /
                (currentTime - lastTimestamp);

        lastCpuTime = cpuTime;
        lastTimestamp = currentTime;

        return Math.min(100.0, usage);
    }

    private long getTotalCpuTime() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = br.readLine(); // 只读取第一行cpu总量
            String[] parts = line.substring(5).trim().split("\\s+");
            long total = 0;
            for (String part : parts) {
                total += Long.parseLong(part);
            }
            return total * (1_000_000_000L / 100); // 转换为纳秒
        }
    }
}
