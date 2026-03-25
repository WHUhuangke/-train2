package com.train.ticketing.app.model;

import com.train.ticketing.domain.OrderStateMachine;
import com.train.ticketing.domain.OrderStatus;

import java.time.Instant;
import java.util.Objects;

public class TicketOrder {
    private final String orderNo;
    private final long userId;
    private final String requestId;
    private final String trainCode;
    private final String seatType;
    private final String passengerId;
    private final int fromStationIndex;
    private final int toStationIndex;
    private OrderStatus status;
    private String reason;
    private Instant statusUpdatedAt;

    public TicketOrder(String orderNo, long userId, String requestId, String trainCode, String seatType,
                       String passengerId, int fromStationIndex, int toStationIndex) {
        this.orderNo = Objects.requireNonNull(orderNo);
        this.userId = userId;
        this.requestId = Objects.requireNonNull(requestId);
        this.trainCode = Objects.requireNonNull(trainCode);
        this.seatType = Objects.requireNonNull(seatType);
        this.passengerId = Objects.requireNonNull(passengerId);
        this.fromStationIndex = fromStationIndex;
        this.toStationIndex = toStationIndex;
        this.status = OrderStatus.CREATING;
        this.reason = "创建订单";
        this.statusUpdatedAt = Instant.now();
    }

    public synchronized void transit(OrderStatus next, String reason, OrderStateMachine stateMachine) {
        stateMachine.assertCanTransit(this.status, next);
        this.status = next;
        this.reason = reason;
        this.statusUpdatedAt = Instant.now();
    }

    public String orderNo() { return orderNo; }
    public long userId() { return userId; }
    public String requestId() { return requestId; }
    public String trainCode() { return trainCode; }
    public String seatType() { return seatType; }
    public String passengerId() { return passengerId; }
    public int fromStationIndex() { return fromStationIndex; }
    public int toStationIndex() { return toStationIndex; }
    public synchronized OrderStatus status() { return status; }
    public synchronized String reason() { return reason; }
    public synchronized Instant statusUpdatedAt() { return statusUpdatedAt; }
}
