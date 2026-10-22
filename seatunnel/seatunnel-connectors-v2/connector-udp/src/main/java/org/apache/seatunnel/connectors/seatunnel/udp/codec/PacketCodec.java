package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import java.util.List;
import java.util.Map;

/** Decode raw UDP bytes into one or more row maps. */
public interface PacketCodec {

    /**
     * @return list of field maps (one packet may expand to multiple rows)
     */
    List<Map<String, Object>> decode(byte[] payload, PacketMeta meta) throws Exception;
}
