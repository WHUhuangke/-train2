package com.train.ticketing.app.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTrainQueryRepository implements TrainQueryRepository {
    private final Map<String, List<String>> trains = new ConcurrentHashMap<>();

    public void init(String travelDate, String fromStation, String toStation, List<String> trainCodes) {
        trains.put(key(travelDate, fromStation, toStation), new ArrayList<>(trainCodes));
    }

    @Override
    public List<String> queryTrainCodes(String travelDate, String fromStation, String toStation) {
        return trains.getOrDefault(key(travelDate, fromStation, toStation), List.of());
    }

    private String key(String travelDate, String fromStation, String toStation) {
        return travelDate + "#" + fromStation + "#" + toStation;
    }
}
