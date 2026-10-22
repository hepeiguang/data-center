/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.meter.metrics;

import io.micrometer.core.instrument.Gauge;
import org.apache.dolphinscheduler.common.utils.OSUtils;

import io.micrometer.core.instrument.MeterRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DefaultMetricsProvider implements MetricsProvider {

    private final MeterRegistry meterRegistry;
    private final ContainerMetrics containerMetrics;

    public DefaultMetricsProvider(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.containerMetrics = new ContainerMetrics();
        // 注册容器CPU指标
        if (isContainerized()) {
        Gauge.builder("container.cpu.usage", containerMetrics, ContainerMetrics::getSystemCpuUsage)
                .register(meterRegistry);
        Gauge.builder("container.process.cpu.usage", containerMetrics, ContainerMetrics::getProcessCpuUsage)
                .register(meterRegistry);
         }
    }

    private SystemMetrics systemMetrics;

    private long lastRefreshTime = 0;

    private static final long SYSTEM_METRICS_REFRESH_INTERVAL = 1_000L;

    @Override
    public SystemMetrics getSystemMetrics() {
        if (System.currentTimeMillis() - lastRefreshTime < SYSTEM_METRICS_REFRESH_INTERVAL) {
            return systemMetrics;
        }

        double systemCpuUsage = meterRegistry.find("container.cpu.usage").gauge() != null
                ? meterRegistry.get("container.cpu.usage").gauge().value()
                : meterRegistry.get("system.cpu.usage").gauge().value();

        double processCpuUsage = meterRegistry.find("container.process.cpu.usage").gauge() != null
                ? meterRegistry.get("container.process.cpu.usage").gauge().value()
                : meterRegistry.get("process.cpu.usage").gauge().value();

        double jvmMemoryUsed = meterRegistry.get("jvm.memory.used").meter().measure().iterator().next().getValue();
        double jvmMemoryMax = meterRegistry.get("jvm.memory.max").meter().measure().iterator().next().getValue();

        long totalSystemMemory = OSUtils.getTotalSystemMemory();
        long systemMemoryAvailable = OSUtils.getSystemAvailableMemoryUsed();

        systemMetrics = SystemMetrics.builder()
                .systemCpuUsagePercentage(systemCpuUsage)
                .jvmCpuUsagePercentage(processCpuUsage)
                .jvmMemoryUsed(jvmMemoryUsed)
                .jvmMemoryMax(jvmMemoryMax)
                .jvmMemoryUsedPercentage(jvmMemoryUsed / jvmMemoryMax)
                .systemMemoryUsed(totalSystemMemory - systemMemoryAvailable)
                .systemMemoryMax(totalSystemMemory)
                .systemMemoryUsedPercentage((double) (totalSystemMemory - systemMemoryAvailable) / totalSystemMemory)
                .build();
        lastRefreshTime = System.currentTimeMillis();
        return systemMetrics;
    }

    // 在DefaultMetricsProvider.java中添加容器检测方法
    private boolean isContainerized() {
        File dockerEnv = new File("/.dockerenv");
        if (dockerEnv.exists()) {
            return true;
        }

        try {
            String cgroup = new String(Files.readAllBytes(Paths.get("/proc/self/cgroup")));
            return cgroup.contains("docker") || cgroup.contains("kubepods");
        } catch (IOException ignored) {
            return false;
        }
    }

}
