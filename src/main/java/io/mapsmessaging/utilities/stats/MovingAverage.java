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

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages a single moving average, accumulates data over the period and then provides an average that moves with time. https://en.wikipedia.org/wiki/Moving_average
 *
 * @author Matthew Buckton
 * @version 1.0
 * @since 1.0
 */
public class MovingAverage {

  @Getter
  private final String name;
  private final List<DataPoint> dataPoints;
  private final long timePeriod;
  private final int expectedEntries;

  private long current;

  /**
   * Creates a new instance over the period of time in TimeUnits
   *
   * @param time Time span to use for the moving average
   * @param unit The time unit that describes the time
   */
  public MovingAverage(int time, TimeUnit unit) {
    this.name = time + "_" + unit.toString();
    timePeriod = unit.toMillis(time);
    dataPoints = new ArrayList<>();
    expectedEntries = time;
    current =0;
  }

  /**
   * Add new data point to the MovingAverage
   *
   * @param data to be added to the MovingAverage
   */
  public void add(long data) {
    long now = System.currentTimeMillis();
    dataPoints.add(new DataPoint(data, now + timePeriod));
    clearData(now);
  }

  /***
   * @return The current moving average for the current data set
   */
  public long getAverage() {
    return current;
  }

  public void update(){
    clearData(System.currentTimeMillis());
    long ave = 0;
    for (DataPoint dataPoint : dataPoints) {
      ave += dataPoint.data;
    }
    current = ave / expectedEntries;
  }

  /**
   * Resets the moving average and removes any outstanding data
   */
  public void reset() {
    dataPoints.clear();
  }


  protected void clearData(long now) {
    dataPoints.removeIf(dataPoint -> dataPoint.expiry < now);
  }

  protected static class DataPoint {

    protected final long data;
    protected final long expiry;

    public DataPoint(long data, long delay) {
      this.data = data;
      expiry = delay;
    }
  }

}
