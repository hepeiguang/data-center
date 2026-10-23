package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;

import com.google.auto.service.AutoService;

@AutoService(PacketCodecProvider.class)
public class BytesPacketCodecProvider implements PacketCodecProvider {

    @Override
    public String name() {
        return "bytes";
    }

    @Override
    public PacketCodec create(UdpSourceConfig config) {
        return new BytesPacketCodec(config.isDecoderIncludeMeta());
    }
}
