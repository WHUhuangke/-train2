package com.train.ticketing.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SegmentInventoryTest {

    @Test
    void shouldDeductAcrossAllSegmentsInRange() {
        SegmentInventory inventory = new SegmentInventory(5, 2);

        Assertions.assertTrue(inventory.tryPreDeduct(0, 2));
        Assertions.assertArrayEquals(new int[]{1, 1, 2, 2}, inventory.snapshot());

        Assertions.assertTrue(inventory.tryPreDeduct(1, 4));
        Assertions.assertArrayEquals(new int[]{1, 0, 1, 1}, inventory.snapshot());

        Assertions.assertFalse(inventory.tryPreDeduct(0, 3));
        Assertions.assertArrayEquals(new int[]{1, 0, 1, 1}, inventory.snapshot());
    }

    @Test
    void shouldCompensateAfterFailure() {
        SegmentInventory inventory = new SegmentInventory(4, 1);
        Assertions.assertTrue(inventory.tryPreDeduct(0, 3));
        inventory.compensate(0, 3);
        Assertions.assertArrayEquals(new int[]{1, 1, 1}, inventory.snapshot());
    }
}
