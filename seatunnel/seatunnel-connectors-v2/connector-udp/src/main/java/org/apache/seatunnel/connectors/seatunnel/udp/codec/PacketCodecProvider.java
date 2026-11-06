package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;

/**
 * SPI for pluggable UDP packet decoders.
 *
 * <p>Register via {@code META-INF/services/...PacketCodecProvider} or set {@code
 * parseMode=custom} + {@code decoderClass} to a provider / {@link PacketCodec} class.
 */
public interface PacketCodecProvider {

    /** Logical name, e.g. {@code string}, {@code bytes}, or a custom id. */
    String name();

    PacketCodec create(UdpSourceConfig config) throws Exception;
}
