package com.train.ticketing.service;

import com.train.ticketing.domain.SegmentInventory;
import com.train.ticketing.service.TicketingWorkflowService.GrabCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

class TicketingWorkflowServiceTest {

    @Test
    void shouldCompensateStockAndSeatWhenOrderCreationFails() {
        TicketingWorkflowService service = new TicketingWorkflowService(new InMemoryIdempotencyStore());
        SegmentInventory inventory = new SegmentInventory(5, 1);
        AtomicBoolean released = new AtomicBoolean(false);

        var result = service.executeGrab(
                new GrabCommand("req-1", "G100", "2nd", 0, 2, "p1"),
                new TicketingWorkflowService.SeatPreemptor() {
                    @Override
                    public boolean tryPreempt(GrabCommand command) {
                        return true;
                    }

                    @Override
                    public void release(GrabCommand command) {
                        released.set(true);
                    }
                },
                inventory,
                command -> {
                    throw new RuntimeException("db down");
                },
                (orderNo, command) -> {
                }
        );

        Assertions.assertFalse(result.success());
        Assertions.assertTrue(released.get());
        Assertions.assertArrayEquals(new int[]{1, 1, 1, 1}, inventory.snapshot());
    }

    @Test
    void shouldRejectDuplicatedRequestByIdempotencyKey() {
        TicketingWorkflowService service = new TicketingWorkflowService(new InMemoryIdempotencyStore());
        SegmentInventory inventory = new SegmentInventory(4, 2);
        GrabCommand command = new GrabCommand("same-request", "G1", "2nd", 0, 1, "p1");

        var first = service.executeGrab(command,
                c -> true,
                inventory,
                c -> "O100",
                (orderNo, c) -> {
                });
        var second = service.executeGrab(command,
                c -> true,
                inventory,
                c -> "O101",
                (orderNo, c) -> {
                });

        Assertions.assertTrue(first.success());
        Assertions.assertFalse(second.success());
        Assertions.assertEquals("重复请求", second.reason());
    }
}
