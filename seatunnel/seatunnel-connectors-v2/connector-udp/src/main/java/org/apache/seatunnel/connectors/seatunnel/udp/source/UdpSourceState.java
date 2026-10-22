package org.apache.seatunnel.connectors.seatunnel.udp.source;

import java.io.Serializable;

public class UdpSourceState implements Serializable {
    // 可以添加状态字段
    // 例如: private long receivedCount;

    @Override
    public String toString() {
        return "UdpSourceState{}";
    }
}
