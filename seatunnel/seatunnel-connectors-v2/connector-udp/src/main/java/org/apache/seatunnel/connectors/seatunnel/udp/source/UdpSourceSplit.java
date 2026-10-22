package org.apache.seatunnel.connectors.seatunnel.udp.source;

import org.apache.seatunnel.api.source.SourceSplit;

/** 用于定义 UDP 数据源的分片逻辑，其核心作用是支持分布式环境下数据读取任务的并行化与容错管理。 */
public class UdpSourceSplit implements SourceSplit {
    private final String splitId;
    private final long timestamp;

    public UdpSourceSplit(String splitId, long timestamp) {
        this.splitId = splitId;
        this.timestamp = timestamp;
    }

    @Override
    public String splitId() {
        return splitId;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "UdpSourceSplit{"
                + "splitId='"
                + splitId
                + '\''
                + "timestamp='"
                + timestamp
                + '\''
                + '}';
    }
}
