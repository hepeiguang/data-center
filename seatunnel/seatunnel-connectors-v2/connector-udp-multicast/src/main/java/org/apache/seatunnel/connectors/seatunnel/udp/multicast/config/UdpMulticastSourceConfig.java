package org.apache.seatunnel.connectors.seatunnel.udp.multicast.config;

import lombok.Getter;
import lombok.ToString;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.utils.NetworkInterfaceUtil;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;

@Getter
@ToString
public class UdpMulticastSourceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String groupAddress;
    private final int port;
    private final NetworkInterface networkInterface;
    private final int soTimeout;
    private final int ttl;
    private final int receiveBufferSize;
    private final int maxPacketSize;
    private final boolean reuseAddress;
    private final boolean replyEnabled;
    private final String replyMessage;
    private final String charsetName;
    private final String format;

    public UdpMulticastSourceConfig(ReadonlyConfig config) {
        this.groupAddress = config.get(UdpMulticastOptions.GROUP_ADDRESS);
        this.port = config.get(UdpMulticastOptions.PORT);
        this.soTimeout = config.get(UdpMulticastOptions.SO_TIMEOUT);
        this.ttl = config.get(UdpMulticastOptions.TTL);
        this.receiveBufferSize = config.get(UdpMulticastOptions.RECEIVE_BUFFER_SIZE);
        this.maxPacketSize = config.get(UdpMulticastOptions.MAX_PACKET_SIZE);
        this.reuseAddress = config.get(UdpMulticastOptions.REUSE_ADDRESS);
        this.replyEnabled = config.get(UdpMulticastOptions.REPLY_ENABLED);
        this.replyMessage = config.get(UdpMulticastOptions.REPLY_MESSAGE);
        this.charsetName = config.get(UdpMulticastOptions.CHARSET);
        this.format = config.get(UdpMulticastOptions.FORMAT);
        String interfaceName = config.get(UdpMulticastOptions.NETWORK_INTERFACE);
        try {
            this.networkInterface = NetworkInterfaceUtil.getNetworkInterface(interfaceName);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isJsonFormat() {
        return format.equalsIgnoreCase("json");
    }

    public Charset getCharset() {
        return Charset.forName(charsetName);
    }

}
