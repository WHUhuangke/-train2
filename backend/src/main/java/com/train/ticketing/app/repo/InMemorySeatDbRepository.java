package com.train.ticketing.app.repo;

import com.train.ticketing.domain.SeatAllocator;
import com.train.ticketing.domain.SeatBitmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySeatDbRepository implements SeatDbRepository {
    private final SeatAllocator allocator = new SeatAllocator();
    private final Map<String, List<SeatAllocator.SeatUnit>> seatMap = new ConcurrentHashMap<>();
    private final Map<String, String> seatBindByOrderScope = new ConcurrentHashMap<>();

    public void init(String trainCode, String seatType, int seatCount, int segmentCount) {
        List<SeatAllocator.SeatUnit> units = new ArrayList<>();
        for (int i = 1; i <= seatCount; i++) {
            units.add(new SeatAllocator.SeatUnit(String.format("%02d", i), new SeatBitmap(segmentCount)));
        }
        seatMap.put(key(trainCode, seatType), units);
    }

    @Override
    public synchronized Optional<String> allocateAndPersist(String trainCode, String seatType, int fromStationIndex, int toStationIndex) {
        List<SeatAllocator.SeatUnit> units = seatMap.get(key(trainCode, seatType));
        if (units == null) {
            throw new IllegalArgumentException("seat map not found");
        }
        Optional<String> seatNo = allocator.allocate(units, fromStationIndex, toStationIndex);
        seatNo.ifPresent(no -> seatBindByOrderScope.put(bindKey(trainCode, seatType, no, fromStationIndex, toStationIndex), no));
        return seatNo;
    }

    @Override
    public synchronized void release(String trainCode, String seatType, String seatNo, int fromStationIndex, int toStationIndex) {
        List<SeatAllocator.SeatUnit> units = seatMap.get(key(trainCode, seatType));
        if (units == null) {
            return;
        }
        for (SeatAllocator.SeatUnit unit : units) {
            if (unit.seatNo().equals(seatNo)) {
                unit.bitmap().release(fromStationIndex, toStationIndex);
                seatBindByOrderScope.remove(bindKey(trainCode, seatType, seatNo, fromStationIndex, toStationIndex));
                return;
            }
        }
    }

    private String key(String trainCode, String seatType) {
        return trainCode + "#" + seatType;
    }

    private String bindKey(String trainCode, String seatType, String seatNo, int from, int to) {
        return trainCode + "#" + seatType + "#" + seatNo + "#" + from + "#" + to;
    }
}
