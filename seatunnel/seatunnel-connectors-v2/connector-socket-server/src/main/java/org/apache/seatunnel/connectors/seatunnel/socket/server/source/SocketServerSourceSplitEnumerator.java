package org.apache.seatunnel.connectors.seatunnel.socket.server.source;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerSourceConfig;

import java.util.Collections;
import java.util.List;

@Slf4j
public class SocketServerSourceSplitEnumerator
        implements SourceSplitEnumerator<SocketServerSourceSplit, SocketServerSourceState> {
    private final Context<SocketServerSourceSplit> context;
    private final SocketServerSourceConfig config;
    private volatile boolean shouldEnumerate;

    public SocketServerSourceSplitEnumerator(Context<SocketServerSourceSplit> context, SocketServerSourceConfig config) {
        this.context = context;
        this.config = config;
        this.shouldEnumerate = true;
    }

    @Override
    public void open() {
        // 初始化操作
    }

    @Override
    public void run() throws Exception {
        while (shouldEnumerate) {
            // In socket server case, we only need to create one split
            SocketServerSourceSplit split = new SocketServerSourceSplit("socket-server-split", config);
            context.assignSplit(0, Collections.singletonList(split));
            context.signalNoMoreSplits(0);
            shouldEnumerate = false;
        }
    }

    @Override
    public void close() {
        shouldEnumerate = false;
    }

    @Override
    public void addSplitsBack(List<SocketServerSourceSplit> splits, int subtaskId) {
    }

    @Override
    public int currentUnassignedSplitSize() {
        return 0;
    }

    @Override
    public void handleSplitRequest(int subtaskId) {
        // 不处理split请求
    }

    @Override
    public void registerReader(int subtaskId) {
    }

    @Override
    public SocketServerSourceState snapshotState(long checkpointId) {
        return new SocketServerSourceState();
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {
        // No need to handle checkpoint complete for socket server
    }
}
