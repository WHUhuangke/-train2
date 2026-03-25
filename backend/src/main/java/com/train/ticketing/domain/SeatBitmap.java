package com.train.ticketing.domain;

import java.util.BitSet;
import java.util.Objects;

/**
 * 座位位图，位下标表示区间段（stationIndex -> stationIndex + 1）。
 * 1 表示该段已被占用。
 */
public final class SeatBitmap {
    private final int segmentCount;
    private final BitSet occupiedSegments;

    public SeatBitmap(int segmentCount) {
        if (segmentCount <= 0) {
            throw new IllegalArgumentException("segmentCount must be positive");
        }
        this.segmentCount = segmentCount;
        this.occupiedSegments = new BitSet(segmentCount);
    }

    private SeatBitmap(int segmentCount, BitSet occupiedSegments) {
        this.segmentCount = segmentCount;
        this.occupiedSegments = occupiedSegments;
    }

    public boolean isAvailable(int fromStationIndex, int toStationIndex) {
        validateInterval(fromStationIndex, toStationIndex);
        return occupiedSegments.nextSetBit(fromStationIndex) < 0
                || occupiedSegments.nextSetBit(fromStationIndex) >= toStationIndex;
    }

    public boolean tryReserve(int fromStationIndex, int toStationIndex) {
        validateInterval(fromStationIndex, toStationIndex);
        if (!isAvailable(fromStationIndex, toStationIndex)) {
            return false;
        }
        occupiedSegments.set(fromStationIndex, toStationIndex);
        return true;
    }

    public void release(int fromStationIndex, int toStationIndex) {
        validateInterval(fromStationIndex, toStationIndex);
        occupiedSegments.clear(fromStationIndex, toStationIndex);
    }

    public String snapshot() {
        StringBuilder builder = new StringBuilder(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            builder.append(occupiedSegments.get(i) ? '1' : '0');
        }
        return builder.toString();
    }

    public SeatBitmap copy() {
        return new SeatBitmap(segmentCount, (BitSet) occupiedSegments.clone());
    }

    private void validateInterval(int fromStationIndex, int toStationIndex) {
        if (fromStationIndex < 0 || toStationIndex > segmentCount || fromStationIndex >= toStationIndex) {
            throw new IllegalArgumentException("invalid interval [" + fromStationIndex + ", " + toStationIndex + ")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeatBitmap that)) return false;
        return segmentCount == that.segmentCount && Objects.equals(occupiedSegments, that.occupiedSegments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segmentCount, occupiedSegments);
    }
}
