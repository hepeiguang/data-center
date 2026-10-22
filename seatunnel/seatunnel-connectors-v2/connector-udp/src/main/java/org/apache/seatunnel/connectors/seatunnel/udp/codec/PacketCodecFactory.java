package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class PacketCodecFactory {

    private PacketCodecFactory() {}

    public static PacketCodec create(UdpSourceConfig config) throws Exception {
        UdpOptions.ParseMode mode = config.getParseMode();
        if (mode == null || mode == UdpOptions.ParseMode.none) {
            return null;
        }
        switch (mode) {
            case js:
                return new JsPacketCodec(config.getScriptPath(), config.getScriptContent());
            case offset_table:
                String table = config.getOffsetTable();
                if ((table == null || table.trim().isEmpty())
                        && config.getOffsetTablePath() != null
                        && !config.getOffsetTablePath().trim().isEmpty()) {
                    table =
                            new String(
                                    Files.readAllBytes(Paths.get(config.getOffsetTablePath())),
                                    StandardCharsets.UTF_8);
                }
                if (table == null || table.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "offset_table mode requires offsetTable or offsetTablePath");
                }
                return new OffsetTableParser(table);
            default:
                return null;
        }
    }
}
