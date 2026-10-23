package org.apache.seatunnel.connectors.seatunnel.udp.source.netty;

import java.net.InetSocketAddress;

/** One datagram copied off the Netty event loop for SeaTunnel poll. */
public final class ReceivedUdpPacket {

    private final byte[] payload;
    private final InetSocketAddress sender;
    private final long receiveTs;

    public ReceivedUdpPacket(byte[] payload, InetSocketAddress sender, long receiveTs) {
        this.payload = payload;
        this.sender = sender;
        this.receiveTs = receiveTs;
    }

    public byte[] getPayload() {
        return payload;
    }

    public InetSocketAddress getSender() {
        return sender;
    }

    public long getReceiveTs() {
        return receiveTs;
    }
}
