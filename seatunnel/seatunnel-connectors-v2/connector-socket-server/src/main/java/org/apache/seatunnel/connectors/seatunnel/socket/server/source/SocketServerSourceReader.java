package org.apache.seatunnel.connectors.seatunnel.socket.server.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerSourceConfig;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class SocketServerSourceReader implements SourceReader<SeaTunnelRow, SocketServerSourceSplit> {

    private final SocketServerSourceConfig config;
    private final SourceReader.Context context;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;

    public SocketServerSourceReader(SourceReader.Context context, SocketServerSourceConfig config, DeserializationSchema<SeaTunnelRow> deserializationSchema) {
        this.config = config;
        this.context = context;
        this.deserializationSchema = deserializationSchema;
    }

    /**
     * 提供Socket Server服务 初始化 Socket Server 数据读取环境，如创建 Socket Server、绑定端口、初始化线程等。 在任务启动时由 SeaTunnel 引擎调用，仅执行一次。
     *
     * @throws Exception
     */
    @Override
    public void open() throws Exception {
        serverSocket = new ServerSocket(config.getPort(), config.getMaxConnections());
        executorService = Executors.newFixedThreadPool(config.getMaxConnections());
        log.info("Socket server started on {}:{}", config.getHost(), config.getPort());
    }

    private void handleClient(Socket clientSocket, Collector<SeaTunnelRow> output) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), config.getCharset()));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(clientSocket.getOutputStream(), config.getCharset()), true)) {
            String line;
            while ((line = reader.readLine()) != null && running.get()) {
                if (this.config.isJsonFormat()) {
                    deserializationSchema.deserialize(line.getBytes(), output);
                } else {
                    ObjectNode node = JsonUtils.createObjectNode();
                    node.put("content", line);
                    deserializationSchema.deserialize(JsonUtils.toJsonString(node).getBytes(), output);
                }
                if (config.isReplyEnabled()) {
                    try {
                        writer.println(config.getReplyMessage());
                        writer.flush();
                    } catch (Exception e) {
                        log.error("Failed to send reply to client {}: {}", clientIp, e.getMessage());
                        break; // Exit loop if we can't send replies
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error handling client connection", e);
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                log.warn("Error closing client socket for {}: {}", clientIp, e.getMessage());
            }
            log.info("Client disconnected: {}", clientIp);
        }
    }

    /**
     * 释放资源，如关闭 Socket、清理线程。 任务正常结束或异常终止时调用，确保无资源泄漏。
     *
     * @throws IOException 异常
     */
    @Override
    public void close() throws IOException {
        running.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
        log.info("Socket server resources closed");
    }

    /**
     * 核心数据读取方法，从 Socket 客户端接收数据包，解析后发送给下游。
     *
     * <p>关键点: 通常运行在循环中，直到无更多数据（流模式）或分片完成（批模式）。 需处理数据反序列化和类型转换。
     *
     * @param output output collector.
     * @throws Exception
     */
    @Override
    public void pollNext(Collector<SeaTunnelRow> output) throws Exception {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket, output));
            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * 保存当前读取状态（如偏移量、分片进度），用于故障恢复时精确恢复。
     *
     * <p>触发时机： 定期（根据 checkpoint.interval）或手动触发检查点时调用。
     *
     * @param checkpointId checkpoint Id.
     * @return
     */
    @Override
    public List<SocketServerSourceSplit> snapshotState(long checkpointId) {
        return Collections.emptyList();
    }

    /**
     * 接收分片管理器（SplitEnumerator）分配的新分片，加入本地待处理队列。
     *
     * <p>使用场景： 动态扩缩容时，SplitEnumerator 将新分片分配给空闲的 Reader。
     *
     * @param splits split checkpoint state.
     */
    @Override
    public void addSplits(List<SocketServerSourceSplit> splits) {
        // Only one split is expected for socket server
    }

    /**
     * 通知 Reader 所有分片已分配完毕，处理完当前分片后可安全退出。
     *
     * <p>协作逻辑： pollNext() 中检查该标记，若为 true 且当前分片处理完，主动结束读取。
     */
    @Override
    public void handleNoMoreSplits() {
    }

    /**
     * 确认检查点已完成，可清理临时状态（如确认 Kafka Offset 提交成功）。
     *
     * <p>SocketServer 场景： 通常无操作，但可记录最新检查点 ID：
     *
     * @param checkpointId
     * @throws Exception
     */
    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
    }
}
