package org.apache.seatunnel.connectors.seatunnel.socket.server.source;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
public class SocketServerSourceState implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, String> offsets;

    public SocketServerSourceState() {
        this.offsets = new HashMap<>();
    }

    public void setOffsets(Map<String, String> offsets) {
        this.offsets = offsets;
    }
}
