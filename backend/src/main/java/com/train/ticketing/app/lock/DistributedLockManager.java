package com.train.ticketing.app.lock;

public interface DistributedLockManager {
    boolean tryLock(String key);

    void unlock(String key);
}
