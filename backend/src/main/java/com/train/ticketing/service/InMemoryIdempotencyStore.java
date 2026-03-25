package com.train.ticketing.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 示例实现：生产应替换为 Redis SETNX + EX。
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {
    private final Map<String, Instant> keyExpireAt = new ConcurrentHashMap<>();

    @Override
    public boolean markIfAbsent(String key, Duration ttl) {
        Instant now = Instant.now();
        cleanupExpired(now);
        Instant expireAt = now.plus(ttl);
        return keyExpireAt.putIfAbsent(key, expireAt) == null;
    }

    @Override
    public void delete(String key) {
        keyExpireAt.remove(key);
    }

    private void cleanupExpired(Instant now) {
        keyExpireAt.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
