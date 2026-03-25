package com.train.ticketing.app.service;

import com.train.ticketing.app.lock.DistributedLockManager;
import com.train.ticketing.app.model.TicketOrder;
import com.train.ticketing.app.model.TicketTaskContext;
import com.train.ticketing.app.mq.InventoryFinalizeMessage;
import com.train.ticketing.app.mq.MessageBus;
import com.train.ticketing.app.mq.SeatFinalizeMessage;
import com.train.ticketing.app.repo.ConsumeLogRepository;
import com.train.ticketing.app.repo.OrderRepository;
import com.train.ticketing.app.repo.SeatDbRepository;
import com.train.ticketing.domain.OrderStateMachine;
import com.train.ticketing.domain.OrderStatus;

import java.util.Optional;

/**
 * 异步座位落库：幂等 + 分布式锁 + 位图更新。
 */
public class SeatFinalizeConsumer {
    private final MessageBus messageBus;
    private final ConsumeLogRepository consumeLogRepository;
    private final SeatDbRepository seatDbRepository;
    private final OrderRepository orderRepository;
    private final GrabTicketService grabTicketService;
    private final DistributedLockManager lockManager;
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public SeatFinalizeConsumer(MessageBus messageBus,
                                ConsumeLogRepository consumeLogRepository,
                                SeatDbRepository seatDbRepository,
                                OrderRepository orderRepository,
                                GrabTicketService grabTicketService,
                                DistributedLockManager lockManager) {
        this.messageBus = messageBus;
        this.consumeLogRepository = consumeLogRepository;
        this.seatDbRepository = seatDbRepository;
        this.orderRepository = orderRepository;
        this.grabTicketService = grabTicketService;
        this.lockManager = lockManager;
    }

    public void consumeOne() {
        SeatFinalizeMessage message = messageBus.pollSeat();
        if (message == null || !consumeLogRepository.markConsumedIfAbsent(message.messageId())) {
            return;
        }

        TicketTaskContext context = message.context();
        String lockKey = "seat:" + context.trainCode() + ":" + context.seatType();
        if (!lockManager.tryLock(lockKey)) {
            messageBus.deadLetter(message);
            return;
        }

        try {
            Optional<String> seatNo = seatDbRepository.allocateAndPersist(
                    context.trainCode(), context.seatType(), context.fromStationIndex(), context.toStationIndex());
            if (seatNo.isEmpty()) {
                failOrder(context.orderNo(), "座位分配失败");
                grabTicketService.compensateRedisPreDeduct(context);
                return;
            }
            messageBus.publishInventory(new InventoryFinalizeMessage("inv-" + context.orderNo(), context, seatNo.get()));
        } catch (Exception ex) {
            failOrder(context.orderNo(), "座位落库异常:" + ex.getMessage());
            grabTicketService.compensateRedisPreDeduct(context);
            messageBus.deadLetter(message);
        } finally {
            lockManager.unlock(lockKey);
        }
    }

    private void failOrder(String orderNo, String reason) {
        TicketOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        order.transit(OrderStatus.TICKET_FAILED, reason, stateMachine);
    }
}
