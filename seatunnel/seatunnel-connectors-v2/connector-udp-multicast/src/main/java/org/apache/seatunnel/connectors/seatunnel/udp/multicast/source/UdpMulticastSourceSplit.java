package org.apache.seatunnel.connectors.seatunnel.udp.multicast.source;

import org.apache.seatunnel.api.source.SourceSplit;

public class UdpMulticastSourceSplit implements SourceSplit {

    private final String splitId;
    private final long timestamp;

    public UdpMulticastSourceSplit(String splitId, long timestamp) {
        this.splitId = splitId;
        this.timestamp = timestamp;
    }

    @Override
    public String splitId() {
        return splitId;
    }

    @Override
    public String toString() {
        return "UdpMulticastSourceSplit{"
                + "splitId='"
                + splitId
                + '\''
                + "timestamp='"
                + timestamp
                + '\''
                + '}';
    }
}
