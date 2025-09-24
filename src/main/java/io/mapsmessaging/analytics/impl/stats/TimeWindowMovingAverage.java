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

package io.mapsmessaging.analytics.impl.stats;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class TimeWindowMovingAverage {

  @Getter
  private final String name;

  private final long windowNanos;
  private final Deque<DataPoint> dataPoints;

  private double runningSum;
  private int runningCount;

  public TimeWindowMovingAverage(int time, TimeUnit unit) {
    this.name = time + "_" + unit;
    this.windowNanos = unit.toNanos(time);
    this.dataPoints = new ArrayDeque<>();
    this.runningSum = 0.0;
    this.runningCount = 0;
  }

  public void add(Number value) {
    long now = System.nanoTime();
    purgeExpired(now);
    double v = value.doubleValue();
    dataPoints.addLast(new DataPoint(v, now + windowNanos));
    runningSum += v;
    runningCount += 1;
  }

  public void update() {
    purgeExpired(System.nanoTime());
  }

  public double getAverage() {
    purgeExpired(System.nanoTime());
    if (runningCount == 0) {
      return 0.0;
    }
    return runningSum / runningCount;
  }

  public void reset() {
    dataPoints.clear();
    runningSum = 0.0;
    runningCount = 0;
  }

  private void purgeExpired(long now) {
    while (!dataPoints.isEmpty() && dataPoints.peekFirst().expiryNanos <= now) {
      DataPoint expired = dataPoints.removeFirst();
      runningSum -= expired.value;
      runningCount -= 1;
    }
  }

  private static final class DataPoint {
    private final double value;
    private final long expiryNanos;

    private DataPoint(double value, long expiryNanos) {
      this.value = value;
      this.expiryNanos = expiryNanos;
    }
  }
}
