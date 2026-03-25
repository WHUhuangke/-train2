package com.train.ticketing.app.lock;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDistributedLockManager implements DistributedLockManager {
    private final Set<String> lockSet = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryLock(String key) {
        return lockSet.add(key);
    }

    @Override
    public void unlock(String key) {
        lockSet.remove(key);
    }
}
