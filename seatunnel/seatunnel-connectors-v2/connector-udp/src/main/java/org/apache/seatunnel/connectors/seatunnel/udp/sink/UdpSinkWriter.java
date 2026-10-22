/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.udp.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.serialization.SerializationSchema;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSinkWriter;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSinkConfig;
import org.apache.seatunnel.format.json.JsonSerializationSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class UdpSinkWriter extends AbstractSinkWriter<SeaTunnelRow, Void> {
    private final UdpSinkConfig config;
    private final SerializationSchema serializationSchema;
    private final DatagramSocket socket;
    private final InetAddress address;

    // 异步发送相关
    private final ExecutorService executorService;
    private final BlockingQueue<byte[]> asyncQueue;
    private final AtomicBoolean running;
    private final AtomicInteger pendingTasks;
    private final int maxQueueSize;

    public UdpSinkWriter(UdpSinkConfig config, SeaTunnelRowType seaTunnelRowType) {
        this.config = config;
        this.serializationSchema = new JsonSerializationSchema(seaTunnelRowType);

        try {
            this.socket = new DatagramSocket();
            this.address = InetAddress.getByName(config.getHost());
            if (config.getTimeoutMs() > 0) {
                this.socket.setSoTimeout(config.getTimeoutMs());
            }
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException("Failed to initialize UDP socket", e);
        }

        // 异步配置
        this.maxQueueSize = config.getMaxQueueSize();
        this.asyncQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.running = new AtomicBoolean(true);
        this.pendingTasks = new AtomicInteger(0);
        this.executorService = Executors.newFixedThreadPool(config.getAsyncThreads());

        // 启动异步发送线程
        startAsyncSender();
        log.info("Udp sink config: {}", config);
    }

    @Override
    public void write(SeaTunnelRow element) throws IOException {
        // 添加到批量缓冲区
        if (element == null) {
            log.warn("Received null row, skipping");
            return;
        }
        log.info("Udp sink receive data: {}", element);
        if (this.config.isAsyncEnabled()) {
            try {
                asyncQueue.put(serializationSchema.serialize(element));
            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
                log.error("Put data to queue failed, ", e);
            }
        } else {
            sendWithRetry(serializationSchema.serialize(element));
        }
    }

    private void sendWithRetry(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, config.getPort());
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= config.getMaxRetries()) {
            try {
                socket.send(packet);
                log.debug("Send data to upd server, ip: {}, port: {}, data: {}",
                        this.address.getHostAddress(), config.getPort(), new String(data));
                if (config.isEnableMessageReply()) {
                    waitForReply();
                }
                return;
            } catch (IOException e) {
                lastException = e;
                retryCount++;
                log.warn("Failed to send UDP packet (attempt {}/{}): {}",
                        retryCount, config.getMaxRetries() + 1, e.getMessage());

                if (retryCount <= config.getMaxRetries()) {
                    try {
                        Thread.sleep(config.getRetryIntervalMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException(String.format(
                "Failed to send UDP message after %d retries", config.getMaxRetries()), lastException);
    }

    private void waitForReply() throws IOException {
        byte[] replyBuffer = new byte[1024];
        DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length);
        socket.receive(replyPacket);
        log.debug("Received reply from server: {}",
                new String(replyPacket.getData(), 0, replyPacket.getLength()));
    }

    private void startAsyncSender() {
        for (int i = 0; i < config.getAsyncThreads(); i++) {
            executorService.submit(() -> {
                while (running.get() || !asyncQueue.isEmpty() || pendingTasks.get() > 0) {
                    try {
                        byte[] data = asyncQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (data != null) {
                            try {
                                sendWithRetry(data);
                            } finally {
                                pendingTasks.decrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Async sender thread interrupted");
                        break;
                    } catch (Exception e) {
                        log.error("Error in async sender thread", e);
                    }
                }
            });
        }
    }

    @Override
    public Optional<Void> prepareCommit() {
        // 等待异步任务完成
        if (config.isAsyncEnabled()) {
            long start = System.currentTimeMillis();
            while (pendingTasks.get() > 0) {
                if (System.currentTimeMillis() - start > config.getMaxWaitMs()) {
                    throw new RuntimeException("Timeout waiting for async tasks to complete");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for async tasks", e);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        // 停止异步发送线程
        running.set(false);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }

        // 关闭socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        log.info("UdpSinkWriter closed");
    }

}
