package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;

import java.nio.charset.Charset;

import com.google.auto.service.AutoService;

@AutoService(PacketCodecProvider.class)
public class StringPacketCodecProvider implements PacketCodecProvider {

    @Override
    public String name() {
        return "string";
    }

    @Override
    public PacketCodec create(UdpSourceConfig config) {
        Charset charset =
                Charset.forName(
                        config.getCharset() == null || config.getCharset().isEmpty()
                                ? "UTF-8"
                                : config.getCharset());
        return new StringPacketCodec(charset, config.isDecoderIncludeMeta());
    }
}
