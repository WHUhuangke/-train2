package com.train.ticketing.app.service;

import com.train.ticketing.app.model.TicketOrder;
import com.train.ticketing.app.model.TicketTaskContext;
import com.train.ticketing.app.mq.MessageBus;
import com.train.ticketing.app.mq.SeatFinalizeMessage;
import com.train.ticketing.app.repo.OrderRepository;
import com.train.ticketing.domain.OrderStateMachine;
import com.train.ticketing.domain.OrderStatus;
import com.train.ticketing.domain.SegmentInventory;
import com.train.ticketing.service.IdempotencyStore;

import java.time.Duration;
import java.util.UUID;

/**
 * 抢票入口：预扣库存 + 创建订单 + 发 seat finalize 消息。
 */
public class GrabTicketService {
    private final IdempotencyStore idempotencyStore;
    private final SegmentInventory redisPreDeductInventory;
    private final OrderRepository orderRepository;
    private final MessageBus messageBus;
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public GrabTicketService(IdempotencyStore idempotencyStore,
                             SegmentInventory redisPreDeductInventory,
                             OrderRepository orderRepository,
                             MessageBus messageBus) {
        this.idempotencyStore = idempotencyStore;
        this.redisPreDeductInventory = redisPreDeductInventory;
        this.orderRepository = orderRepository;
        this.messageBus = messageBus;
    }

    public GrabResult grab(GrabRequest request) {
        String idempotentKey = "grab:" + request.requestId();
        if (!idempotencyStore.markIfAbsent(idempotentKey, Duration.ofMinutes(5))) {
            return GrabResult.reject("重复请求");
        }

        if (!redisPreDeductInventory.tryPreDeduct(request.fromStationIndex(), request.toStationIndex())) {
            return GrabResult.reject("余票不足");
        }

        String orderNo = UUID.randomUUID().toString();
        TicketOrder order = new TicketOrder(orderNo, request.userId(), request.requestId(), request.trainCode(),
                request.seatType(), request.passengerId(), request.fromStationIndex(), request.toStationIndex());
        order.transit(OrderStatus.TICKETING, "进入出票中", stateMachine);
        orderRepository.save(order);

        TicketTaskContext context = new TicketTaskContext(orderNo, request.trainCode(), request.seatType(),
                request.fromStationIndex(), request.toStationIndex(), request.userId(), request.passengerId(),
                request.requestId());
        messageBus.publishSeat(new SeatFinalizeMessage("seat-" + orderNo, context));
        return GrabResult.accept(orderNo);
    }

    public void compensateRedisPreDeduct(TicketTaskContext context) {
        redisPreDeductInventory.compensate(context.fromStationIndex(), context.toStationIndex());
    }

    public record GrabRequest(String requestId, long userId, String trainCode, String seatType,
                              int fromStationIndex, int toStationIndex, String passengerId) {
    }

    public record GrabResult(boolean success, String orderNo, String reason) {
        static GrabResult accept(String orderNo) {
            return new GrabResult(true, orderNo, null);
        }

        static GrabResult reject(String reason) {
            return new GrabResult(false, null, reason);
        }
    }
}
