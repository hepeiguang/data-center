package com.lhyf.dataasset.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Tiny per-key TTL cache; avoids adding a cache dependency to keep the service light. */
public class TtlCache<K, V> {

    private static final class Entry<V> {
        final V value;
        final long expiresAt;

        Entry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public TtlCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public V get(K key, Supplier<V> loader) {
        long now = System.currentTimeMillis();
        Entry<V> existing = entries.get(key);
        if (existing != null && existing.expiresAt > now) {
            return existing.value;
        }
        V value = loader.get();
        entries.put(key, new Entry<>(value, now + ttlMillis));
        return value;
    }

    public void invalidate(K key) {
        entries.remove(key);
    }

    public void clear() {
        entries.clear();
    }
}
