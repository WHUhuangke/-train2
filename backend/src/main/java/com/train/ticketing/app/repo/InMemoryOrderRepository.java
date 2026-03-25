package com.train.ticketing.app.repo;

import com.train.ticketing.app.model.TicketOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, TicketOrder> orderByNo = new ConcurrentHashMap<>();

    @Override
    public void save(TicketOrder order) {
        orderByNo.put(order.orderNo(), order);
    }

    @Override
    public Optional<TicketOrder> findByOrderNo(String orderNo) {
        return Optional.ofNullable(orderByNo.get(orderNo));
    }

    @Override
    public List<TicketOrder> findByUserId(long userId) {
        List<TicketOrder> result = new ArrayList<>();
        for (TicketOrder value : orderByNo.values()) {
            if (value.userId() == userId) {
                result.add(value);
            }
        }
        return result;
    }
}
