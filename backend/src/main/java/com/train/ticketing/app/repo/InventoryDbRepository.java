package com.train.ticketing.app.repo;

public interface InventoryDbRepository {
    boolean deduct(String trainCode, String seatType, int fromStationIndex, int toStationIndex);

    void compensate(String trainCode, String seatType, int fromStationIndex, int toStationIndex);

    int minRemain(String trainCode, String seatType, int fromStationIndex, int toStationIndex);
}
