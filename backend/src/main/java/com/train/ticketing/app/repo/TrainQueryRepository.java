package com.train.ticketing.app.repo;

import java.util.List;

public interface TrainQueryRepository {
    List<String> queryTrainCodes(String travelDate, String fromStation, String toStation);
}
