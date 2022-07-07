package io.mapsmessaging.engine.stats;

import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Statistics {

  private final List<LinkedMovingAverages> averageList;

  public Statistics() {
    averageList = new ArrayList<>();
  }

  public List<LinkedMovingAverages> getAverageList() {
    return new ArrayList<>(averageList);
  }

  public LinkedMovingAverages create(ACCUMULATOR accumulator, String name, String units) {
    LinkedMovingAverages linkedMovingAverages = MovingAverageFactory.getInstance().createLinked(accumulator, name, 1, 5, 4, TimeUnit.MINUTES, units);
    averageList.add(linkedMovingAverages);
    return linkedMovingAverages;
  }

}
