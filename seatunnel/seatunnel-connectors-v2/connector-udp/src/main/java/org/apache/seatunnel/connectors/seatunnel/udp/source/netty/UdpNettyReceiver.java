package org.apache.seatunnel.connectors.seatunnel.udp.source.netty;

import lombok.extern.slf4j.Slf4j;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Netty-based UDP bind/receive. Packets are copied onto a bounded queue so SeaTunnel {@code
 * pollNext} stays off the event loop. Optional auto-reply is sent from the Netty thread.
 */
@Slf4j
public class UdpNettyReceiver implements AutoCloseable {

    private final String host;
    private final int port;
    private final int packetBufferSize;
    private final int workerThreads;
    private final int queueCapacity;
    private final int soRcvBuf;
    private final boolean replyEnabled;
    private final byte[] replyBytes;

    private final BlockingQueue<ReceivedUdpPacket> queue;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private EventLoopGroup group;
    private Channel channel;

    public UdpNettyReceiver(
            String host,
            int port,
            int packetBufferSize,
            int workerThreads,
            int queueCapacity,
            int soRcvBuf,
            boolean replyEnabled,
            String replyMessage,
            Charset charset) {
        this.host = host;
        this.port = port;
        this.packetBufferSize = Math.max(512, packetBufferSize);
        this.workerThreads = Math.max(1, workerThreads);
        this.queueCapacity = Math.max(16, queueCapacity);
        this.soRcvBuf = soRcvBuf > 0 ? soRcvBuf : 1024 * 1024;
        this.replyEnabled = replyEnabled;
        this.replyBytes =
                replyEnabled && replyMessage != null
                        ? replyMessage.getBytes(charset)
                        : new byte[0];
        this.queue = new ArrayBlockingQueue<>(this.queueCapacity);
    }

    public void start() throws InterruptedException {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        group = new NioEventLoopGroup(workerThreads);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, soRcvBuf)
                .option(ChannelOption.RCVBUF_ALLOCATOR,
                        new io.netty.channel.FixedRecvByteBufAllocator(packetBufferSize))
                .handler(
                        new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            protected void initChannel(NioDatagramChannel ch) {
                                ch.pipeline().addLast(new InboundHandler());
                            }
                        });

        InetSocketAddress address = new InetSocketAddress(host, port);
        channel = bootstrap.bind(address).sync().channel();
        log.info(
                "UDP Netty receiver bound on {}:{} (workers={}, queue={}, soRcvBuf={})",
                host,
                port,
                workerThreads,
                queueCapacity,
                soRcvBuf);
    }

    public ReceivedUdpPacket poll(long timeoutMs) throws InterruptedException {
        return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        running.set(false);
        if (channel != null) {
            try {
                channel.close().syncUninterruptibly();
            } catch (Exception e) {
                log.warn("Close UDP channel failed: {}", e.toString());
            }
        }
        if (group != null) {
            group.shutdownGracefully(0, 2, TimeUnit.SECONDS);
        }
        queue.clear();
    }

    private final class InboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            if (!running.get()) {
                return;
            }
            ByteBuf content = msg.content();
            int len = content.readableBytes();
            byte[] payload = new byte[len];
            content.getBytes(content.readerIndex(), payload);

            InetSocketAddress sender = msg.sender();
            ReceivedUdpPacket packet =
                    new ReceivedUdpPacket(payload, sender, System.currentTimeMillis());
            if (!queue.offer(packet)) {
                log.warn(
                        "UDP receive queue full (cap={}), drop packet from {}",
                        queueCapacity,
                        sender);
            }

            if (replyEnabled && replyBytes.length > 0 && sender != null) {
                ByteBuf buf = ctx.alloc().buffer(replyBytes.length);
                buf.writeBytes(replyBytes);
                ctx.writeAndFlush(new DatagramPacket(buf, sender));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("UDP Netty handler error", cause);
        }
    }
}
