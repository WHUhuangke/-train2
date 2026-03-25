package com.train.ticketing.domain;

public enum OrderStatus {
    CREATING(1),
    TICKETING(2),
    WAIT_PAY(3),
    PAY_SUCCESS(4),
    TICKET_FAILED(5),
    CREATE_FAILED(6);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
