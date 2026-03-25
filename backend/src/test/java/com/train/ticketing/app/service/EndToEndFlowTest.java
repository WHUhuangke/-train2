package com.train.ticketing.app.service;

import com.train.ticketing.app.lock.InMemoryDistributedLockManager;
import com.train.ticketing.app.mq.MessageBus;
import com.train.ticketing.app.notify.InMemorySseNotifier;
import com.train.ticketing.app.repo.InMemoryConsumeLogRepository;
import com.train.ticketing.app.repo.InMemoryInventoryDbRepository;
import com.train.ticketing.app.repo.InMemoryOrderRepository;
import com.train.ticketing.app.repo.InMemorySeatDbRepository;
import com.train.ticketing.domain.OrderStatus;
import com.train.ticketing.domain.SegmentInventory;
import com.train.ticketing.service.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EndToEndFlowTest {

    @Test
    void shouldCompleteGrabToPaySuccess() {
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        InMemoryInventoryDbRepository inventoryDb = new InMemoryInventoryDbRepository();
        InMemorySeatDbRepository seatDb = new InMemorySeatDbRepository();
        InMemorySseNotifier notifier = new InMemorySseNotifier();
        MessageBus bus = new MessageBus();

        inventoryDb.init("G100", "2nd", 5, 2);
        seatDb.init("G100", "2nd", 2, 4);

        GrabTicketService grabService = new GrabTicketService(
                new InMemoryIdempotencyStore(),
                new SegmentInventory(5, 2),
                orderRepo,
                bus
        );

        SeatFinalizeConsumer seatConsumer = new SeatFinalizeConsumer(
                bus,
                new InMemoryConsumeLogRepository(),
                seatDb,
                orderRepo,
                grabService,
                new InMemoryDistributedLockManager()
        );

        InventoryFinalizeConsumer inventoryConsumer = new InventoryFinalizeConsumer(
                bus,
                new InMemoryConsumeLogRepository(),
                inventoryDb,
                orderRepo,
                seatDb,
                grabService,
                notifier
        );

        var result = grabService.grab(new GrabTicketService.GrabRequest(
                "r1", 101L, "G100", "2nd", 0, 2, "p1"
        ));
        Assertions.assertTrue(result.success());

        seatConsumer.consumeOne();
        inventoryConsumer.consumeOne();

        var order = orderRepo.findByOrderNo(result.orderNo()).orElseThrow();
        Assertions.assertEquals(OrderStatus.WAIT_PAY, order.status());
        Assertions.assertTrue(notifier.messages(101L).stream().anyMatch(m -> m.startsWith("GRAB_SUCCESS")));

        PaymentService paymentService = new PaymentService(orderRepo, notifier, new InMemoryIdempotencyStore());
        paymentService.paySuccess(order.orderNo(), "pay-1");
        Assertions.assertEquals(OrderStatus.PAY_SUCCESS, order.status());
    }

    @Test
    void shouldFailOrderWhenInventoryDbDeductFails() {
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        InMemoryInventoryDbRepository inventoryDb = new InMemoryInventoryDbRepository();
        InMemorySeatDbRepository seatDb = new InMemorySeatDbRepository();
        InMemorySseNotifier notifier = new InMemorySseNotifier();
        MessageBus bus = new MessageBus();

        inventoryDb.init("G200", "2nd", 5, 0);
        seatDb.init("G200", "2nd", 1, 4);

        GrabTicketService grabService = new GrabTicketService(
                new InMemoryIdempotencyStore(),
                new SegmentInventory(5, 1),
                orderRepo,
                bus
        );

        var result = grabService.grab(new GrabTicketService.GrabRequest(
                "r2", 102L, "G200", "2nd", 0, 2, "p2"
        ));

        SeatFinalizeConsumer seatConsumer = new SeatFinalizeConsumer(
                bus,
                new InMemoryConsumeLogRepository(),
                seatDb,
                orderRepo,
                grabService,
                new InMemoryDistributedLockManager()
        );

        InventoryFinalizeConsumer inventoryConsumer = new InventoryFinalizeConsumer(
                bus,
                new InMemoryConsumeLogRepository(),
                inventoryDb,
                orderRepo,
                seatDb,
                grabService,
                notifier
        );

        seatConsumer.consumeOne();
        inventoryConsumer.consumeOne();

        var order = orderRepo.findByOrderNo(result.orderNo()).orElseThrow();
        Assertions.assertEquals(OrderStatus.TICKET_FAILED, order.status());
        Assertions.assertTrue(notifier.messages(102L).stream().anyMatch(m -> m.startsWith("GRAB_FAIL")));
    }
}
