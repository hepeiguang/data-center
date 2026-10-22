package org.apache.seatunnel.connectors.seatunnel.http.server.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.source.SourceSplitEnumerator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;


@Slf4j
public class HttpServerSourceEnumerator implements SourceSplitEnumerator<HttpServerSourceSplit, HttpServerSourceState> {

    private final Context<HttpServerSourceSplit> context;
    private boolean splitAssigned = false;

    public HttpServerSourceEnumerator(Context<HttpServerSourceSplit> context) {
        this.context = context;
    }

    public HttpServerSourceEnumerator(Context<HttpServerSourceSplit> context,
                                      @Nullable HttpServerSourceState checkpointState) {
        this(context);
        if (checkpointState != null) {
            this.splitAssigned = checkpointState.isSplitAssigned();
        }
    }

    @Override
    public void open() {
        log.info("Opening HTTP Server source enumerator");
    }

    @Override
    public void run() throws Exception {
        // No-op for HTTP server as it's event-driven
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void addSplitsBack(List<HttpServerSourceSplit> splits, int subtaskId) {
        if (!splits.isEmpty()) {
            splitAssigned = false;
        }
    }

    @Override
    public int currentUnassignedSplitSize() {
        return splitAssigned ? 0 : 1;
    }

    @Override
    public void handleSplitRequest(int subtaskId) {
        if (!splitAssigned) {
            context.assignSplit(subtaskId, new HttpServerSourceSplit());
            splitAssigned = true;
        } else {
            context.signalNoMoreSplits(subtaskId);
        }
    }

    @Override
    public void registerReader(int subtaskId) {
    }

    @Override
    public HttpServerSourceState snapshotState(long checkpointId) throws Exception {
        return new HttpServerSourceState(splitAssigned);
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {

    }
}
