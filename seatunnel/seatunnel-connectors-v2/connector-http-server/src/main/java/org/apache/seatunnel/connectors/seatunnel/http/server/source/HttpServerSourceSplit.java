package org.apache.seatunnel.connectors.seatunnel.http.server.source;

import org.apache.seatunnel.api.source.SourceSplit;

public class HttpServerSourceSplit implements SourceSplit {

    private static final String SPLIT_ID = "http_server_split";

    @Override
    public String splitId() {
        return SPLIT_ID;
    }
}
