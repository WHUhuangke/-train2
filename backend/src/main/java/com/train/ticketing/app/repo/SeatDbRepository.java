package com.train.ticketing.app.repo;

import java.util.Optional;

public interface SeatDbRepository {
    Optional<String> allocateAndPersist(String trainCode, String seatType, int fromStationIndex, int toStationIndex);

    void release(String trainCode, String seatType, String seatNo, int fromStationIndex, int toStationIndex);
}
