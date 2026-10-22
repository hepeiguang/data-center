package org.apache.seatunnel.connectors.seatunnel.udp.multicast.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastSourceConfig;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class UdpMulticastSourceReader implements SourceReader<SeaTunnelRow, UdpMulticastSourceSplit> {

    private final SourceReader.Context context;
    private final UdpMulticastSourceConfig config;
    private MulticastSocket socket;
    private volatile boolean running = true;
    private final byte[] receiveBuffer;
    private final ExecutorService replyExecutor;
    private final Charset charset;
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;

    public UdpMulticastSourceReader(SourceReader.Context context, UdpMulticastSourceConfig config,
                                    DeserializationSchema<SeaTunnelRow> deserializationSchema) {
        this.context = context;
        this.config = config;
        this.receiveBuffer = new byte[config.getMaxPacketSize()];
        this.replyExecutor = config.isReplyEnabled() ?
                Executors.newSingleThreadExecutor() : null;
        this.charset = config.getCharset();
        this.deserializationSchema = deserializationSchema;
    }

    @Override
    public void open() throws Exception {
        InetAddress group = InetAddress.getByName(config.getGroupAddress());
        socket = new MulticastSocket(config.getPort());

        if (config.getNetworkInterface() != null) {
            socket.setNetworkInterface(config.getNetworkInterface());
        }

        //socket.setSoTimeout(config.getSoTimeout());
        socket.setReuseAddress(config.isReuseAddress());
        socket.setReceiveBufferSize(config.getReceiveBufferSize());
        socket.setTimeToLive(config.getTtl());
        socket.joinGroup(group);

        log.info("UDP multicast source reader started on {}:{} with timeout={}ms",
                config.getGroupAddress(), config.getPort(), config.getSoTimeout());
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (socket != null) {
            try {
                socket.leaveGroup(InetAddress.getByName(config.getGroupAddress()));
            } catch (Exception e) {
                log.warn("Failed to leave multicast group", e);
            }
            socket.close();
        }
        if (replyExecutor != null) {
            replyExecutor.shutdownNow();
        }
    }

    @Override
    public void pollNext(Collector<SeaTunnelRow> output) throws Exception {
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        try {
            socket.receive(packet);
            String message = new String(
                    packet.getData(),
                    packet.getOffset(),
                    packet.getLength(),
                    charset);
            if (config.isJsonFormat()) {
                deserializationSchema.deserialize(message.getBytes(), output);
            } else {
                ObjectNode node = JsonUtils.createObjectNode();
                node.put("content", message);
                deserializationSchema.deserialize(JsonUtils.toJsonString(node).getBytes(), output);
            }
            // Handle reply if enabled
            if (config.isReplyEnabled() && replyExecutor != null) {
                InetSocketAddress sourceAddress = (InetSocketAddress) packet.getSocketAddress();
                replyExecutor.submit(() -> sendReply(sourceAddress));
            }
        } catch (IOException e) {
            if (running) {
                throw e;
            }
        }
    }

    private void sendReply(SocketAddress address) {
        try {
            byte[] replyData = config.getReplyMessage().getBytes(charset);
            DatagramPacket replyPacket = new DatagramPacket(
                    replyData,
                    replyData.length,
                    address);
            socket.send(replyPacket);
            log.debug("Sent reply to {}", address);
        } catch (IOException e) {
            log.warn("Failed to send reply to {}", address, e);
        }
    }

    @Override
    public List<UdpMulticastSourceSplit> snapshotState(long checkpointId) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public void addSplits(List<UdpMulticastSourceSplit> splits) {

    }

    @Override
    public void handleNoMoreSplits() {

    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {

    }
}
