package com.train.ticketing.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 按“最先可用”策略分配座位。
 */
public class SeatAllocator {

    public Optional<String> allocate(List<SeatUnit> seatUnits, int fromStationIndex, int toStationIndex) {
        return seatUnits.stream()
                .sorted(Comparator.comparing(SeatUnit::seatNo))
                .filter(seat -> seat.bitmap().tryReserve(fromStationIndex, toStationIndex))
                .map(SeatUnit::seatNo)
                .findFirst();
    }

    public record SeatUnit(String seatNo, SeatBitmap bitmap) {
    }
}
