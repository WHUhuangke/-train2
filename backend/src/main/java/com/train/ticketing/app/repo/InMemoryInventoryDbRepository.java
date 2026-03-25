package com.train.ticketing.app.repo;

import com.train.ticketing.domain.SegmentInventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInventoryDbRepository implements InventoryDbRepository {
    private final Map<String, SegmentInventory> inventoryMap = new ConcurrentHashMap<>();

    public void init(String trainCode, String seatType, int stationCount, int initRemain) {
        inventoryMap.put(key(trainCode, seatType), new SegmentInventory(stationCount, initRemain));
    }

    @Override
    public boolean deduct(String trainCode, String seatType, int fromStationIndex, int toStationIndex) {
        return resolve(trainCode, seatType).tryPreDeduct(fromStationIndex, toStationIndex);
    }

    @Override
    public void compensate(String trainCode, String seatType, int fromStationIndex, int toStationIndex) {
        resolve(trainCode, seatType).compensate(fromStationIndex, toStationIndex);
    }

    @Override
    public int minRemain(String trainCode, String seatType, int fromStationIndex, int toStationIndex) {
        return resolve(trainCode, seatType).queryMinRemain(fromStationIndex, toStationIndex);
    }

    private SegmentInventory resolve(String trainCode, String seatType) {
        SegmentInventory inventory = inventoryMap.get(key(trainCode, seatType));
        if (inventory == null) {
            throw new IllegalArgumentException("inventory not found");
        }
        return inventory;
    }

    private String key(String trainCode, String seatType) {
        return trainCode + "#" + seatType;
    }
}
