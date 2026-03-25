package com.train.ticketing.app.service;

import com.train.ticketing.app.model.TicketOrder;
import com.train.ticketing.app.repo.OrderRepository;

import java.util.List;

public class OrderQueryService {
    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public TicketOrder getOne(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
    }

    public List<TicketOrder> list(long userId) {
        return orderRepository.findByUserId(userId);
    }
}
