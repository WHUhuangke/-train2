package com.train.ticketing.app.service;

import com.train.ticketing.app.model.TicketOrder;
import com.train.ticketing.app.notify.SseNotifier;
import com.train.ticketing.app.repo.OrderRepository;
import com.train.ticketing.domain.OrderStateMachine;
import com.train.ticketing.domain.OrderStatus;
import com.train.ticketing.service.IdempotencyStore;

import java.time.Duration;

public class PaymentService {
    private final OrderRepository orderRepository;
    private final SseNotifier notifier;
    private final IdempotencyStore idempotencyStore;
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public PaymentService(OrderRepository orderRepository, SseNotifier notifier, IdempotencyStore idempotencyStore) {
        this.orderRepository = orderRepository;
        this.notifier = notifier;
        this.idempotencyStore = idempotencyStore;
    }

    public boolean paySuccess(String orderNo, String paymentTraceNo) {
        if (!idempotencyStore.markIfAbsent("pay:" + paymentTraceNo, Duration.ofHours(1))) {
            return true;
        }

        TicketOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        if (order.status() == OrderStatus.PAY_SUCCESS) {
            return true;
        }
        order.transit(OrderStatus.PAY_SUCCESS, "支付成功", stateMachine);
        notifier.notifyUser(order.userId(), "PAY_SUCCESS", "orderNo=" + orderNo);
        return true;
    }

    public void timeoutClose(String orderNo) {
        TicketOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        if (order.status() != OrderStatus.WAIT_PAY) {
            return;
        }
        order.transit(OrderStatus.TICKET_FAILED, "超时未支付", stateMachine);
        notifier.notifyUser(order.userId(), "PAY_TIMEOUT", "orderNo=" + orderNo);
    }
}
