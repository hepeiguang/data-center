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
import org.apache.seatunnel.connectors.seatunnel.udp.source.netty.ReceivedUdpPacket;
import org.apache.seatunnel.connectors.seatunnel.udp.source.netty.UdpNettyReceiver;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
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
    private final Charset charset;

    private UdpNettyReceiver nettyReceiver;
    private DatagramSocket jdkSocket;
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
        this.charset =
                Charset.forName(
                        config.getCharset() == null || config.getCharset().isEmpty()
                                ? "UTF-8"
                                : config.getCharset());
        try {
            this.packetCodec = PacketCodecFactory.create(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init UDP packet codec", e);
        }
    }

    @Override
    public void open() throws Exception {
        if (config.getTransport() == UdpOptions.Transport.jdk) {
            this.jdkSocket =
                    new DatagramSocket(new InetSocketAddress(config.getHost(), config.getPort()));
            jdkSocket.setReceiveBufferSize(config.getSoRcvBuf());
            log.info(
                    "UDP source opened with JDK DatagramSocket on {}:{}",
                    config.getHost(),
                    config.getPort());
            return;
        }
        this.nettyReceiver =
                new UdpNettyReceiver(
                        config.getHost(),
                        config.getPort(),
                        config.getBufferSize(),
                        config.getWorkerThreads(),
                        config.getReceiveQueueSize(),
                        config.getSoRcvBuf(),
                        config.isReplyEnabled(),
                        config.getReplyMessage(),
                        charset);
        nettyReceiver.start();
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (nettyReceiver != null) {
            nettyReceiver.close();
            nettyReceiver = null;
        }
        if (jdkSocket != null && !jdkSocket.isClosed()) {
            jdkSocket.close();
            jdkSocket = null;
        }
    }

    @Override
    public void pollNext(Collector<SeaTunnelRow> output) throws Exception {
        if (nettyReceiver != null) {
            pollNetty(output);
        } else {
            pollJdk(output);
        }
    }

    private void pollNetty(Collector<SeaTunnelRow> output) throws Exception {
        while (running) {
            ReceivedUdpPacket packet = nettyReceiver.poll(1000L);
            if (packet == null) {
                continue;
            }
            InetSocketAddress sender = packet.getSender();
            PacketMeta meta =
                    new PacketMeta(
                            sender != null && sender.getAddress() != null
                                    ? sender.getAddress().getHostAddress()
                                    : "",
                            sender != null ? sender.getPort() : 0,
                            packet.getPayload().length,
                            packet.getReceiveTs());
            handlePayload(packet.getPayload(), meta, output);
        }
    }

    private void pollJdk(Collector<SeaTunnelRow> output) throws Exception {
        byte[] buffer = new byte[config.getBufferSize()];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running) {
            jdkSocket.receive(packet);
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

            handlePayload(payload, meta, output);

            if (config.isReplyEnabled()) {
                byte[] responseData = config.getReplyMessage().getBytes(charset);
                DatagramPacket responsePacket =
                        new DatagramPacket(
                                responseData,
                                responseData.length,
                                packet.getAddress(),
                                packet.getPort());
                jdkSocket.send(responsePacket);
            }
        }
    }

    private void handlePayload(byte[] payload, PacketMeta meta, Collector<SeaTunnelRow> output)
            throws Exception {
        try {
            if (packetCodec != null) {
                List<Map<String, Object>> rows = packetCodec.decode(payload, meta);
                if (rows == null) {
                    return;
                }
                for (Map<String, Object> row : rows) {
                    String json = JsonUtils.toJsonString(row);
                    byte[] bytes;
                    if (isSimpleContentSchema()) {
                        ObjectNode node = JsonUtils.createObjectNode();
                        node.put("content", json);
                        bytes = JsonUtils.toJsonString(node).getBytes(charset);
                    } else {
                        bytes = json.getBytes(charset);
                    }
                    deserializationSchema.deserialize(bytes, output);
                }
            } else {
                String message = new String(payload, charset);
                log.debug("Receive udp data: {}", message);
                if (UdpOptions.Format.JSON.equals(config.getFormat())) {
                    deserializationSchema.deserialize(message.getBytes(charset), output);
                } else {
                    ObjectNode node = JsonUtils.createObjectNode();
                    node.put("content", message);
                    deserializationSchema.deserialize(
                            JsonUtils.toJsonString(node).getBytes(charset), output);
                }
            }
        } catch (Exception e) {
            if (config.getParseErrorHandleWay() == UdpOptions.ParseErrorHandleWay.skip) {
                log.warn(
                        "Skip UDP packet from {}:{} due to decode error: {}",
                        meta.getRemoteHost(),
                        meta.getRemotePort(),
                        e.toString());
                return;
            }
            throw e;
        }
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
