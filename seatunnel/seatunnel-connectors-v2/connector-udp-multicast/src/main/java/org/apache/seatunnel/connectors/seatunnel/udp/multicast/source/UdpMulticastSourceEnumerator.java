package org.apache.seatunnel.connectors.seatunnel.udp.multicast.source;

import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastSourceConfig;

import java.io.IOException;
import java.util.List;

public class UdpMulticastSourceEnumerator implements SourceSplitEnumerator<UdpMulticastSourceSplit, UdpMulticastSourceState> {

    private final SourceSplitEnumerator.Context<UdpMulticastSourceSplit> context;
    private final UdpMulticastSourceConfig config;

    public UdpMulticastSourceEnumerator(Context<UdpMulticastSourceSplit> context, UdpMulticastSourceConfig config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public void open() {

    }

    @Override
    public void run() throws Exception {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void addSplitsBack(List<UdpMulticastSourceSplit> splits, int subtaskId) {

    }

    @Override
    public int currentUnassignedSplitSize() {
        return 0;
    }

    @Override
    public void handleSplitRequest(int subtaskId) {

    }

    @Override
    public void registerReader(int subtaskId) {

    }

    @Override
    public UdpMulticastSourceState snapshotState(long checkpointId) throws Exception {
        return new UdpMulticastSourceState();
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {

    }
}
