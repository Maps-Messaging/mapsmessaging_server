/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.utilities.stats;

import io.mapsmessaging.utilities.stats.processors.AdderDataProcessor;
import io.mapsmessaging.utilities.stats.processors.AverageDataProcessor;
import io.mapsmessaging.utilities.stats.processors.DataProcessor;
import io.mapsmessaging.utilities.stats.processors.DifferenceDataProcessor;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Creates a MovingAverage instance that manages the data summing based on the ACCUMULATOR allocated to the MovingAverage
 *
 * @author Matthew Buckton
 * @version 1.0
 * @since 1.0
 */

public class MovingAverageFactory {

  private static final MovingAverageFactory instance = new MovingAverageFactory();

  public static MovingAverageFactory getInstance() {
    return instance;
  }

  @Getter
  public enum ACCUMULATOR {
    ADD("Adder", "Adds all incoming data, useful for tick type stats"),
    DIFF("Difference", "Subtracts the current from the last entry and adds it, useful for dealing with totals"),
    AVE("Average", "Maintains an average");

    private final String name;
    private final String description;

    ACCUMULATOR(String name, String description) {
      this.name = name;
      this.description = description;
    }

  }

  protected final List<LinkedMovingAverages> movingAverages;
  private ScheduledFuture<?> scheduledTask;

  public MovingAverageFactory() {
    movingAverages = new ArrayList<>();
  }

  /**
   * Closes and cancels the scheduler for the moving average
   */
  public void close(LinkedMovingAverages movingAverage) {
    movingAverages.remove(movingAverage);
    if (movingAverages.isEmpty()) {
      scheduledTask.cancel(false);
    }
  }

  /**
   * Creates a new LinkedMovingAverage using the specified Accumulator
   *
   * @param accumulator Accumulator to use to manages the data
   * @param name Name of the moving average
   * @param startPeriod Initial time period that spans the moving average
   * @param periodIncrements Incremental periods from the start period
   * @param totalPeriods Total number of periods to link
   * @param timeUnit The time Unit that is used to define the time periods above
   * @param unitName The name of units being measured, time, messages, things that we are counting
   * @return a newly created LinkedMovingAverages
   */
  public LinkedMovingAverages createLinked(ACCUMULATOR accumulator, String name, int startPeriod, int periodIncrements, int totalPeriods, TimeUnit timeUnit, String unitName) {
    int[] entries = new int[totalPeriods];
    for (int x = 0; x < totalPeriods; x++) {
      entries[x] = startPeriod + (periodIncrements * x);
    }
    return createLinked(accumulator, name, entries, timeUnit, unitName);
  }

  public LinkedMovingAverages createLinked(ACCUMULATOR accumulator, String name, int[] entries, TimeUnit timeUnit, String unitName) {
    DataProcessor processor;
    switch (accumulator) {
      case DIFF:
        processor = new DifferenceDataProcessor();
        break;

      case AVE:
        processor = new AverageDataProcessor();
        break;

      case ADD:
      default:
        processor = new AdderDataProcessor();
        break;
    }
    //
    // The first entry needs to deal with the incoming data, the "linked" moving averages are all SUM based
    //
    LinkedMovingAverages movingAverage = new LinkedMovingAverages(processor, name, entries, timeUnit, unitName);
    if (movingAverages.isEmpty()) {
      scheduledTask = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new ScheduleRunner(), 1, 1, TimeUnit.MINUTES);
    }
    movingAverages.add(movingAverage);
    return movingAverage;
  }

  private final class ScheduleRunner implements Runnable {

    @Override
    public void run() {
      for (LinkedMovingAverages movingAverage : movingAverages) {
        movingAverage.update();
      }
    }
  }

}
