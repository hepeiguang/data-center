package org.apache.seatunnel.connectors.seatunnel.socket.server.source;

import lombok.Getter;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerSourceConfig;

/**
 * 用于定义 socket server 数据源的分片逻辑，其核心作用是支持分布式环境下数据读取任务的并行化与容错管理。
 */
public class SocketServerSourceSplit implements SourceSplit {
    private final String splitId;
    @Getter
    private final SocketServerSourceConfig config;

    public SocketServerSourceSplit(String splitId, SocketServerSourceConfig config) {
        this.splitId = splitId;
        this.config = config;
    }

    @Override
    public String splitId() {
        return splitId;
    }

}
