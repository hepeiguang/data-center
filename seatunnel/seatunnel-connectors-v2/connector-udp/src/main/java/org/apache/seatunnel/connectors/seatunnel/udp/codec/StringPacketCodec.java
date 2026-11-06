package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Default decoder: payload → UTF-8 (or configured charset) string field {@code content}. */
public class StringPacketCodec implements PacketCodec {

    private final Charset charset;
    private final boolean includeMeta;

    public StringPacketCodec(Charset charset, boolean includeMeta) {
        this.charset = charset;
        this.includeMeta = includeMeta;
    }

    public StringPacketCodec() {
        this(Charset.forName("UTF-8"), false);
    }

    @Override
    public List<Map<String, Object>> decode(byte[] payload, PacketMeta meta) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("content", new String(payload, charset));
        if (includeMeta && meta != null) {
            row.putAll(meta.toMap());
        }
        return Collections.singletonList(row);
    }
}
