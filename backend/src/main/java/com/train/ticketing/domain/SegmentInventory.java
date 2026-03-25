package com.train.ticketing.domain;

import java.util.Arrays;

/**
 * 区间库存：每一段都有剩余座位数，预扣时按区间内最小值判断是否可售。
 */
public class SegmentInventory {
    private final int[] segmentRemain;

    public SegmentInventory(int stationCount, int initRemainPerSegment) {
        if (stationCount < 2) {
            throw new IllegalArgumentException("stationCount must be >= 2");
        }
        this.segmentRemain = new int[stationCount - 1];
        Arrays.fill(this.segmentRemain, initRemainPerSegment);
    }

    public synchronized boolean tryPreDeduct(int fromStationIndex, int toStationIndex) {
        validateInterval(fromStationIndex, toStationIndex);
        for (int i = fromStationIndex; i < toStationIndex; i++) {
            if (segmentRemain[i] <= 0) {
                return false;
            }
        }
        for (int i = fromStationIndex; i < toStationIndex; i++) {
            segmentRemain[i]--;
        }
        return true;
    }

    public synchronized void compensate(int fromStationIndex, int toStationIndex) {
        validateInterval(fromStationIndex, toStationIndex);
        for (int i = fromStationIndex; i < toStationIndex; i++) {
            segmentRemain[i]++;
        }
    }

    public synchronized int queryMinRemain(int fromStationIndex, int toStationIndex) {
        validateInterval(fromStationIndex, toStationIndex);
        int min = Integer.MAX_VALUE;
        for (int i = fromStationIndex; i < toStationIndex; i++) {
            min = Math.min(min, segmentRemain[i]);
        }
        return min;
    }

    private void validateInterval(int fromStationIndex, int toStationIndex) {
        if (fromStationIndex < 0 || toStationIndex > segmentRemain.length || fromStationIndex >= toStationIndex) {
            throw new IllegalArgumentException("invalid interval");
        }
    }

    public synchronized int[] snapshot() {
        return Arrays.copyOf(segmentRemain, segmentRemain.length);
    }
}
