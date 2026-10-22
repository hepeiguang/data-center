package org.apache.seatunnel.connectors.seatunnel.http.server.source;

import java.io.Serializable;

/**
 * State for HTTP server source
 */
public class HttpServerSourceState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean splitAssigned;

    public HttpServerSourceState(boolean splitAssigned) {
        this.splitAssigned = splitAssigned;
    }

    public boolean isSplitAssigned() {
        return splitAssigned;
    }
}
