package org.apache.seatunnel.connectors.seatunnel.udp.multicast.utils;

import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * Utility class for network interface operations.
 */
public class NetworkInterfaceUtil {

    public static NetworkInterface getNetworkInterface(String interfaceName) throws SocketException {
        if (interfaceName == null || interfaceName.isEmpty()) {
            return null;
        }
        NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
        if (networkInterface == null) {
            throw new IllegalArgumentException("Network interface not found: " + interfaceName);
        }

        return networkInterface;
    }
}
