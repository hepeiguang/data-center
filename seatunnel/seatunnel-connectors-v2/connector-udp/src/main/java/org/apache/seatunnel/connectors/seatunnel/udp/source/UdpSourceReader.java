package org.apache.seatunnel.connectors.seatunnel.udp.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.connectors.seatunnel.udp.codec.PacketCodec;
import org.apache.seatunnel.connectors.seatunnel.udp.codec.PacketCodecFactory;
import org.apache.seatunnel.connectors.seatunnel.udp.codec.PacketMeta;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class UdpSourceReader implements SourceReader<SeaTunnelRow, UdpSourceSplit> {
    private final SourceReader.Context context;
    private final UdpSourceConfig config;
    private final SeaTunnelRowType rowType;
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;
    private final PacketCodec packetCodec;

    private DatagramSocket socket;
    private volatile boolean running = true;

    public UdpSourceReader(
            Context context,
            UdpSourceConfig config,
            SeaTunnelRowType rowType,
            DeserializationSchema<SeaTunnelRow> deserializationSchema) {
        this.context = context;
        this.config = config;
        this.rowType = rowType;
        this.deserializationSchema = deserializationSchema;
        try {
            this.packetCodec = PacketCodecFactory.create(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init UDP packet codec", e);
        }
    }

    @Override
    public void open() throws Exception {
        this.socket = new DatagramSocket(new InetSocketAddress(config.getHost(), config.getPort()));
        socket.setReceiveBufferSize(1024 * 1024);
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void pollNext(Collector<SeaTunnelRow> output) throws Exception {
        byte[] buffer = new byte[config.getBufferSize()];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running) {
            socket.receive(packet);
            byte[] payload = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), payload, 0, packet.getLength());

            PacketMeta meta =
                    new PacketMeta(
                            packet.getAddress() != null
                                    ? packet.getAddress().getHostAddress()
                                    : "",
                            packet.getPort(),
                            packet.getLength(),
                            System.currentTimeMillis());

            try {
                emitPacket(output, payload, meta);
            } catch (Exception e) {
                handleParseError(meta, e);
                continue;
            }

            if (config.isReplyEnabled()) {
                byte[] responseData = config.getReplyMessage().getBytes(config.getCharset());
                DatagramPacket responsePacket =
                        new DatagramPacket(
                                responseData,
                                responseData.length,
                                packet.getAddress(),
                                packet.getPort());
                socket.send(responsePacket);
            }
        }
    }

    private void emitPacket(Collector<SeaTunnelRow> output, byte[] payload, PacketMeta meta)
            throws Exception {
        if (packetCodec != null) {
            List<Map<String, Object>> rows = packetCodec.decode(payload, meta);
            for (Map<String, Object> row : rows) {
                String json = JsonUtils.toJsonString(row);
                byte[] bytes;
                if (isSimpleContentSchema()) {
                    ObjectNode node = JsonUtils.createObjectNode();
                    node.put("content", json);
                    bytes = JsonUtils.toJsonString(node).getBytes(config.getCharset());
                } else {
                    bytes = json.getBytes(config.getCharset());
                }
                deserializationSchema.deserialize(bytes, output);
            }
        } else {
            String message = new String(payload, config.getCharset());
            log.debug("Receive udp data: {}", message);
            if (UdpOptions.Format.JSON.equals(config.getFormat())) {
                deserializationSchema.deserialize(message.getBytes(config.getCharset()), output);
            } else {
                ObjectNode node = JsonUtils.createObjectNode();
                node.put("content", message);
                deserializationSchema.deserialize(
                        JsonUtils.toJsonString(node).getBytes(config.getCharset()), output);
            }
        }
    }

    private void handleParseError(PacketMeta meta, Exception e) throws Exception {
        UdpOptions.ParseErrorHandleWay way = config.getParseErrorHandleWay();
        if (way == null) {
            way = UdpOptions.ParseErrorHandleWay.skip;
        }
        if (way == UdpOptions.ParseErrorHandleWay.fail) {
            throw e;
        }
        log.warn(
                "Skip bad UDP packet from {}:{} len={}: {}",
                meta.getRemoteHost(),
                meta.getRemotePort(),
                meta.getLength(),
                e.toString());
    }

    @Override
    public List<UdpSourceSplit> snapshotState(long checkpointId) {
        return Collections.singletonList(
                new UdpSourceSplit("udp-split-0", System.currentTimeMillis()));
    }

    @Override
    public void addSplits(List<UdpSourceSplit> splits) {}

    @Override
    public void handleNoMoreSplits() {}

    @Override
    public void notifyCheckpointComplete(long checkpointId) {}

    private boolean isSimpleContentSchema() {
        return rowType != null
                && rowType.getTotalFields() == 1
                && "content".equals(rowType.getFieldNames()[0]);
    }
}
