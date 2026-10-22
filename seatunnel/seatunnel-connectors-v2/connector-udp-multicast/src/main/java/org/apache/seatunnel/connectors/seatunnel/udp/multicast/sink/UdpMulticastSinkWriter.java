package org.apache.seatunnel.connectors.seatunnel.udp.multicast.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSinkWriter;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastSinkConfig;
import org.apache.seatunnel.format.json.JsonSerializationSchema;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class UdpMulticastSinkWriter extends AbstractSinkWriter<SeaTunnelRow, Void> {

    private final UdpMulticastSinkConfig config;
    private final SeaTunnelRowType seaTunnelRowType;
    private final MulticastSocket socket;
    private final InetAddress groupAddress;
    private final Charset charset;

    // Batch processing
    private final BlockingQueue<byte[]> batchQueue;
    private final ScheduledExecutorService batchFlusher;

    // Async processing
    private final ExecutorService asyncSender;
    private final BlockingQueue<byte[]> asyncQueue;
    private final AtomicInteger pendingAsyncTasks = new AtomicInteger(0);
    private final SerializationSchema serializationSchema;

    // Reply handling
    private final Object replyLock = new Object();


    public UdpMulticastSinkWriter(UdpMulticastSinkConfig config, SeaTunnelRowType seaTunnelRowType) {
        this.config = config;
        this.seaTunnelRowType = seaTunnelRowType;
        this.serializationSchema = new JsonSerializationSchema(seaTunnelRowType);
        this.charset = config.getCharset();

        try {
            // Initialize network components in constructor
            this.groupAddress = InetAddress.getByName(config.getGroupAddress());
            this.socket = new MulticastSocket();

            if (config.getNetworkInterface() != null) {
                socket.setNetworkInterface(config.getNetworkInterface());
            }

            socket.setSoTimeout(config.getSoTimeout());
            socket.setSendBufferSize(config.getSendBufferSize());
            socket.setTimeToLive(config.getTtl());

            log.info("UDP multicast sink writer initialized for {}:{} with async={}, batchSize={}",
                    config.getGroupAddress(), config.getPort(),
                    config.isAsyncEnabled(), config.getBatchSize());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize UDP multicast sink writer", e);
        }

        // Initialize batch processing
        this.batchQueue = new LinkedBlockingQueue<>(config.getBatchQueueSize());
        if (config.getBatchIntervalMs() > 0) {
            this.batchFlusher = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "udp-multicast-batch-flusher");
                thread.setDaemon(true);
                return thread;
            });
            this.batchFlusher.scheduleAtFixedRate(
                    this::flushBatch,
                    config.getBatchIntervalMs(),
                    config.getBatchIntervalMs(),
                    TimeUnit.MILLISECONDS);
        } else {
            this.batchFlusher = null;
        }

        // Initialize async processing
        if (config.isAsyncEnabled()) {
            this.asyncSender = Executors.newFixedThreadPool(config.getAsyncThreads(), r -> {
                Thread thread = new Thread(r, "udp-multicast-async-sender");
                thread.setDaemon(true);
                return thread;
            });
            this.asyncQueue = new LinkedBlockingQueue<>(config.getAsyncQueueSize());

            // Start async sender threads
            for (int i = 0; i < config.getAsyncThreads(); i++) {
                asyncSender.submit(this::asyncSendLoop);
            }
        } else {
            this.asyncSender = null;
            this.asyncQueue = null;
        }
    }

    @Override
    public void write(SeaTunnelRow element) throws IOException {
        if (config.isAsyncEnabled()) {
            // Submit to async queue
            try {
                boolean offered = asyncQueue.offer(
                        serializationSchema.serialize(element),
                        config.getAsyncWaitNanos(),
                        TimeUnit.NANOSECONDS);
                if (!offered) {
                    throw new IOException("Async send queue is full after waiting " +
                            config.getAsyncWaitNanos() + " nanos");
                }
                pendingAsyncTasks.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting to add to async queue", e);
            }
        } else if (config.getBatchSize() > 1) {
            // Batch the message
            try {
                boolean offered = batchQueue.offer(
                        serializationSchema.serialize(element),
                        config.getAsyncWaitNanos(),
                        TimeUnit.NANOSECONDS);
                if (!offered) {
                    throw new IOException("Batch queue is full after waiting " +
                            config.getAsyncWaitNanos() + " nanos");
                }
                if (batchQueue.size() >= config.getBatchSize()) {
                    flushBatch();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting to add to batch queue", e);
            }
        } else {
            // Send immediately
            sendWithRetry(serializationSchema.serialize(element));
        }
    }

    private void asyncSendLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] task = asyncQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    sendWithRetry(task);
                    pendingAsyncTasks.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in async sender thread", e);
            }
        }
    }

    private void flushBatch() {
        if (batchQueue.isEmpty()) {
            return;
        }
        List<byte[]> currentBatch = new ArrayList<>(config.getBatchSize());
        batchQueue.drainTo(currentBatch, config.getBatchSize());

        if (!currentBatch.isEmpty()) {
            try {
                for (byte[] message : currentBatch) {
                    sendWithRetry(message);
                }
            } catch (IOException e) {
                log.error("Failed to send batched UDP packets", e);
            }
        }
    }

    private void sendWithRetry(byte[] message) throws IOException {
        int attempt = 0;
        IOException lastException = null;

        while (attempt <= config.getMaxRetries()) {
            try {
                sendPacket(message);
                return;
            } catch (IOException e) {
                lastException = e;
                attempt++;
                if (attempt <= config.getMaxRetries()) {
                    log.warn("Send attempt {} failed, retrying in {} ms: {}",
                            attempt, config.getRetryIntervalMs(), e.getMessage());
                    try {
                        Thread.sleep(config.getRetryIntervalMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry", ie);
                    }
                }
            }
        }
        throw new IOException("Failed after " + config.getMaxRetries() + " attempts", lastException);
    }

    private void sendPacket(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                groupAddress,
                config.getPort());
        socket.send(packet);
        if (config.isWaitForReply()) {
            waitForReply();
        }
    }

    private void waitForReply() throws IOException {
        if (!config.isWaitForReply()) {
            return;
        }
        synchronized (replyLock) {
            try {
                byte[] replyBuffer = new byte[config.getMaxPacketSize()];
                DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length);
                socket.setSoTimeout(config.getReplyTimeoutMs());
                socket.receive(replyPacket);
                String reply = new String(
                        replyPacket.getData(),
                        replyPacket.getOffset(),
                        replyPacket.getLength(),
                        charset);
                log.debug("Received reply: {}", reply);
            } catch (SocketTimeoutException e) {
                throw new IOException("Timeout waiting for reply after " +
                        config.getReplyTimeoutMs() + " ms", e);
            } finally {
                socket.setSoTimeout(config.getSoTimeout());
            }
        }
    }

    @Override
    public Optional<Void> prepareCommit() {
        // Wait for all async tasks to complete
        if (config.isAsyncEnabled()) {
            long startWait = System.currentTimeMillis();
            while (pendingAsyncTasks.get() > 0) {
                try {
                    Thread.sleep(10);
                    // Add timeout to prevent infinite wait
                    if (System.currentTimeMillis() - startWait > config.getAsyncWaitNanos() / 1_000_000) {
                        throw new RuntimeException("Timeout waiting for async tasks to complete");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for async tasks", e);
                }
            }
        }
        // Flush any remaining messages in batch
        if (!batchQueue.isEmpty()) {
            flushBatch();
        }
        return Optional.empty();
    }

    @Override
    public void close() throws IOException {
        // Shutdown batch flusher
        if (batchFlusher != null) {
            batchFlusher.shutdownNow();
            try {
                if (!batchFlusher.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Batch flusher did not terminate properly");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Shutdown async sender
        if (asyncSender != null) {
            asyncSender.shutdownNow();
            try {
                if (!asyncSender.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Async sender did not terminate properly");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Close socket
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                log.warn("Error while closing socket", e);
            }
        }
    }

}
