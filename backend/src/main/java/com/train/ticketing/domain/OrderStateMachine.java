package com.train.ticketing.domain;

import java.util.Map;
import java.util.Set;

public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.CREATING, Set.of(OrderStatus.TICKETING, OrderStatus.CREATE_FAILED),
            OrderStatus.TICKETING, Set.of(OrderStatus.WAIT_PAY, OrderStatus.TICKET_FAILED),
            OrderStatus.WAIT_PAY, Set.of(OrderStatus.PAY_SUCCESS, OrderStatus.TICKET_FAILED),
            OrderStatus.PAY_SUCCESS, Set.of(),
            OrderStatus.TICKET_FAILED, Set.of(),
            OrderStatus.CREATE_FAILED, Set.of()
    );

    public void assertCanTransit(OrderStatus from, OrderStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("illegal status transition: " + from + " -> " + to);
        }
    }
}
