package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Default decoder: payload → Base64 string field {@code content} (+ optional meta). */
public class BytesPacketCodec implements PacketCodec {

    private final boolean includeMeta;

    public BytesPacketCodec(boolean includeMeta) {
        this.includeMeta = includeMeta;
    }

    public BytesPacketCodec() {
        this(false);
    }

    @Override
    public List<Map<String, Object>> decode(byte[] payload, PacketMeta meta) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("content", Base64.getEncoder().encodeToString(payload));
        row.put("length", payload == null ? 0 : payload.length);
        if (includeMeta && meta != null) {
            row.putAll(meta.toMap());
        }
        return Collections.singletonList(row);
    }
}
