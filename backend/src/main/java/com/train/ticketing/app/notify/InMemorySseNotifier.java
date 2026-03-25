package com.train.ticketing.app.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySseNotifier implements SseNotifier {
    private final Map<Long, List<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void notifyUser(long userId, String event, String payload) {
        messages.computeIfAbsent(userId, ignored -> new ArrayList<>())
                .add(event + ":" + payload);
    }

    public List<String> messages(long userId) {
        return messages.getOrDefault(userId, List.of());
    }
}
