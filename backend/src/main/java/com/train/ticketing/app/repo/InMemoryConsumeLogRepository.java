package com.train.ticketing.app.repo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConsumeLogRepository implements ConsumeLogRepository {
    private final Set<String> consumed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markConsumedIfAbsent(String messageId) {
        return consumed.add(messageId);
    }
}
