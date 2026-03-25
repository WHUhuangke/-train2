package com.train.ticketing.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SeatBitmapTest {

    @Test
    void shouldReserveOnlyWhenNoOverlap() {
        SeatBitmap bitmap = new SeatBitmap(6);
        Assertions.assertTrue(bitmap.tryReserve(1, 4));
        Assertions.assertFalse(bitmap.tryReserve(3, 5));
        Assertions.assertTrue(bitmap.tryReserve(4, 6));
        Assertions.assertEquals("011111", bitmap.snapshot());
    }

    @Test
    void shouldReleaseReservedSegments() {
        SeatBitmap bitmap = new SeatBitmap(5);
        bitmap.tryReserve(0, 5);
        bitmap.release(2, 4);
        Assertions.assertEquals("11001", bitmap.snapshot());
        Assertions.assertTrue(bitmap.isAvailable(2, 4));
    }
}
