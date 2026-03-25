package com.train.ticketing.app.notify;

public interface SseNotifier {
    void notifyUser(long userId, String event, String payload);
}
