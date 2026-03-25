package com.train.ticketing.app.repo;

public interface ConsumeLogRepository {
    boolean markConsumedIfAbsent(String messageId);
}
