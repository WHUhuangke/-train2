package com.train.ticketing.app.model;

public record TicketTaskContext(
        String orderNo,
        String trainCode,
        String seatType,
        int fromStationIndex,
        int toStationIndex,
        long userId,
        String passengerId,
        String requestId
) {
}
