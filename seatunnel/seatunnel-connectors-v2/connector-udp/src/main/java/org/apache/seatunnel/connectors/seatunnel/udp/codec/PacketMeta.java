package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** UDP packet metadata passed into codecs / JS parse(). */
public class PacketMeta implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String remoteHost;
    private final int remotePort;
    private final int length;
    private final long ts;

    public PacketMeta(String remoteHost, int remotePort, int length, long ts) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.length = length;
        this.ts = ts;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getLength() {
        return length;
    }

    public long getTs() {
        return ts;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("remoteHost", remoteHost);
        m.put("remotePort", remotePort);
        m.put("length", length);
        m.put("ts", ts);
        return m;
    }
}
