package com.train.ticketing.app.repo;

import com.train.ticketing.app.model.TicketOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(TicketOrder order);

    Optional<TicketOrder> findByOrderNo(String orderNo);

    List<TicketOrder> findByUserId(long userId);
}
