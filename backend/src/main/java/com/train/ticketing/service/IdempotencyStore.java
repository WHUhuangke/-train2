package com.train.ticketing.service;

import java.time.Duration;

public interface IdempotencyStore {
    /**
     * @return true 表示首次写入，false 表示重复请求。
     */
    boolean markIfAbsent(String key, Duration ttl);

    void delete(String key);
}
