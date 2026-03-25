package com.train.ticketing.app.service;

import com.train.ticketing.app.model.TicketOrder;
import com.train.ticketing.app.model.TicketTaskContext;
import com.train.ticketing.app.mq.InventoryFinalizeMessage;
import com.train.ticketing.app.mq.MessageBus;
import com.train.ticketing.app.notify.SseNotifier;
import com.train.ticketing.app.repo.ConsumeLogRepository;
import com.train.ticketing.app.repo.InventoryDbRepository;
import com.train.ticketing.app.repo.OrderRepository;
import com.train.ticketing.app.repo.SeatDbRepository;
import com.train.ticketing.domain.OrderStateMachine;
import com.train.ticketing.domain.OrderStatus;

/**
 * 异步余票落库：扣减 DB 余票 -> 订单待支付 -> SSE 通知。
 */
public class InventoryFinalizeConsumer {
    private final MessageBus messageBus;
    private final ConsumeLogRepository consumeLogRepository;
    private final InventoryDbRepository inventoryDbRepository;
    private final OrderRepository orderRepository;
    private final SeatDbRepository seatDbRepository;
    private final GrabTicketService grabTicketService;
    private final SseNotifier notifier;
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public InventoryFinalizeConsumer(MessageBus messageBus,
                                     ConsumeLogRepository consumeLogRepository,
                                     InventoryDbRepository inventoryDbRepository,
                                     OrderRepository orderRepository,
                                     SeatDbRepository seatDbRepository,
                                     GrabTicketService grabTicketService,
                                     SseNotifier notifier) {
        this.messageBus = messageBus;
        this.consumeLogRepository = consumeLogRepository;
        this.inventoryDbRepository = inventoryDbRepository;
        this.orderRepository = orderRepository;
        this.seatDbRepository = seatDbRepository;
        this.grabTicketService = grabTicketService;
        this.notifier = notifier;
    }

    public void consumeOne() {
        InventoryFinalizeMessage message = messageBus.pollInventory();
        if (message == null || !consumeLogRepository.markConsumedIfAbsent(message.messageId())) {
            return;
        }

        TicketTaskContext context = message.context();
        try {
            boolean deducted = inventoryDbRepository.deduct(context.trainCode(), context.seatType(),
                    context.fromStationIndex(), context.toStationIndex());
            if (!deducted) {
                failAndCompensate(message, "数据库余票不足");
                return;
            }

            TicketOrder order = orderRepository.findByOrderNo(context.orderNo())
                    .orElseThrow(() -> new IllegalArgumentException("order not found"));
            order.transit(OrderStatus.WAIT_PAY, "出票成功，待支付", stateMachine);
            notifier.notifyUser(context.userId(), "GRAB_SUCCESS", "orderNo=" + context.orderNo());
        } catch (Exception ex) {
            failAndCompensate(message, "库存落库异常:" + ex.getMessage());
            messageBus.deadLetter(message);
        }
    }

    private void failAndCompensate(InventoryFinalizeMessage message, String reason) {
        TicketTaskContext context = message.context();
        TicketOrder order = orderRepository.findByOrderNo(context.orderNo())
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        order.transit(OrderStatus.TICKET_FAILED, reason, stateMachine);

        seatDbRepository.release(context.trainCode(), context.seatType(), message.seatNo(),
                context.fromStationIndex(), context.toStationIndex());
        grabTicketService.compensateRedisPreDeduct(context);
        notifier.notifyUser(context.userId(), "GRAB_FAIL", "orderNo=" + context.orderNo() + ",reason=" + reason);
    }
}
