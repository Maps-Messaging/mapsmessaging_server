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


import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

public class MovingAverageStatistics extends BaseStatistics {

  private final TimeWindowMovingAverage movingAverage1Min;
  private final TimeWindowMovingAverage movingAverage5Min;
  private final TimeWindowMovingAverage movingAverage10Min;
  private final TimeWindowMovingAverage movingAverage15Min;

  public MovingAverageStatistics() {
    this.movingAverage1Min = new TimeWindowMovingAverage(1, TimeUnit.MINUTES);
    this.movingAverage5Min = new TimeWindowMovingAverage(5, TimeUnit.MINUTES);
    this.movingAverage10Min = new TimeWindowMovingAverage(10, TimeUnit.MINUTES);
    this.movingAverage15Min = new TimeWindowMovingAverage(15, TimeUnit.MINUTES);
  }

  @Override
  public void reset() {
    super.reset();
    if(movingAverage1Min != null) {
      movingAverage1Min.reset();
      movingAverage5Min.reset();
      movingAverage10Min.reset();
      movingAverage15Min.reset();
    }
  }

  @Override
  protected void update(double currentValue) {
    super.update(currentValue);
    movingAverage1Min.add(currentValue);
    movingAverage5Min.add(currentValue);
    movingAverage10Min.add(currentValue);
    movingAverage15Min.add(currentValue);
  }

  @Override
  protected void addSubclassJson(JsonObject jsonObject) {
    movingAverage1Min.update();
    movingAverage5Min.update();
    movingAverage10Min.update();
    movingAverage15Min.update();

    jsonObject.addProperty("1m", movingAverage1Min.getAverage());
    jsonObject.addProperty("5m", movingAverage5Min.getAverage());
    jsonObject.addProperty("10m", movingAverage10Min.getAverage());
    jsonObject.addProperty("15m", movingAverage15Min.getAverage());
  }

  @Override
  public Statistics create() {
    return new MovingAverageStatistics();
  }

  @Override
  public String getName() {
    return "MovingAverage";
  }

  @Override
  public String getDescription() {
    return "Time-window moving averages over 1, 5, 10 and 15 minutes.";
  }
}
