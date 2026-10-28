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

package org.apache.dolphinscheduler.dataapi.security;

import org.apache.dolphinscheduler.dataapi.config.DataApiProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Per-subject token bucket. In-memory and per-instance; nginx adds a coarse IP-level limit.
 * For strict multi-instance quotas, replace with a Redis-backed limiter.
 */
@Service
public class RateLimitService {

    private static final long IDLE_NANOS = 15L * 60L * 1_000_000_000L;

    private final DataApiProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(DataApiProperties properties) {
        this.properties = properties;
    }

    public boolean tryAcquire(String subjectKey) {
        int permitsPerMinute = properties.getRateLimitPermitsPerMinute();
        if (permitsPerMinute <= 0 || subjectKey == null) {
            return true;
        }
        Bucket bucket = buckets.computeIfAbsent(subjectKey, key -> new Bucket(
                Math.max(1, properties.getRateLimitBurst()),
                permitsPerMinute / 60.0));
        return bucket.tryAcquire();
    }

    @Scheduled(fixedDelay = 300000L)
    public void evictIdle() {
        long now = System.nanoTime();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccess > IDLE_NANOS);
    }

    private static final class Bucket {

        private double tokens;
        private final double capacity;
        private final double refillPerSecond;
        private long lastRefill;
        private volatile long lastAccess;

        private Bucket(double capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillPerSecond = refillPerSecond;
            this.lastRefill = System.nanoTime();
            this.lastAccess = this.lastRefill;
        }

        private synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefill) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillPerSecond);
            lastRefill = now;
            lastAccess = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
