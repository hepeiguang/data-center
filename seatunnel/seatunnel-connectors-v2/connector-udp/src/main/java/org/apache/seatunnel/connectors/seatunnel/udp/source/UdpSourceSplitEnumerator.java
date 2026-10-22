package org.apache.seatunnel.connectors.seatunnel.udp.source;

import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;

import java.util.List;

public class UdpSourceSplitEnumerator
        implements SourceSplitEnumerator<UdpSourceSplit, UdpSourceState> {
    private final SourceSplitEnumerator.Context<UdpSourceSplit> context;
    private final UdpSourceConfig config;

    public UdpSourceSplitEnumerator(Context<UdpSourceSplit> context, UdpSourceConfig config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public void open() {
        // 初始化操作
    }

    @Override
    public void run() throws Exception {
        // do nothing
    }

    @Override
    public void close() {
        // 清理资源
    }

    @Override
    public void addSplitsBack(List<UdpSourceSplit> splits, int subtaskId) {
        // do nothing
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
        // 注册reader
    }

    @Override
    public UdpSourceState snapshotState(long checkpointId) {
        return new UdpSourceState();
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {
        // 检查点完成通知
    }
}
