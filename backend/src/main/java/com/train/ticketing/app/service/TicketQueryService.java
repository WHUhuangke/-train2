package com.train.ticketing.app.service;

import com.train.ticketing.app.repo.InventoryDbRepository;
import com.train.ticketing.app.repo.TrainQueryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TicketQueryService {
    private final TrainQueryRepository trainQueryRepository;
    private final InventoryDbRepository inventoryDbRepository;
    private final Map<String, List<QueryResult>> cache = new ConcurrentHashMap<>();

    public TicketQueryService(TrainQueryRepository trainQueryRepository,
                              InventoryDbRepository inventoryDbRepository) {
        this.trainQueryRepository = trainQueryRepository;
        this.inventoryDbRepository = inventoryDbRepository;
    }

    public List<QueryResult> query(String date, String from, String to, String seatType,
                                   int fromStationIndex, int toStationIndex) {
        String cacheKey = date + "#" + from + "#" + to + "#" + seatType;
        List<QueryResult> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<QueryResult> results = new ArrayList<>();
        for (String trainCode : trainQueryRepository.queryTrainCodes(date, from, to)) {
            int remain = inventoryDbRepository.minRemain(trainCode, seatType, fromStationIndex, toStationIndex);
            results.add(new QueryResult(trainCode, seatType, remain));
        }
        cache.put(cacheKey, results);
        return results;
    }

    public void evict(String date, String from, String to, String seatType) {
        cache.remove(date + "#" + from + "#" + to + "#" + seatType);
    }

    public record QueryResult(String trainCode, String seatType, int remain) {
    }
}
