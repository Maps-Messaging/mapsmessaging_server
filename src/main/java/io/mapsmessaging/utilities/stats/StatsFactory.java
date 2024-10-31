/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.utilities.stats;

import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;

public class StatsFactory {

  @Getter
  @Setter
  private static StatsType defaultType = StatsType.BASIC;

  public static Stats create(StatsType type, String name, String unitName, MovingAverageFactory.ACCUMULATOR accumulator, int startPeriod, int periodIncrements, int totalPeriods, TimeUnit timeUnit){
    switch (type){
      case BASIC:
        return new SimpleStats(name, startPeriod, timeUnit, unitName);
      case FULL:
        return MovingAverageFactory.getInstance().createLinked(accumulator, name, startPeriod, periodIncrements, totalPeriods, timeUnit, unitName);
      case NONE:
      default:
        return new NoStats(name, unitName);
    }
  }

  public static Stats create(StatsType type, String name, String unitName, MovingAverageFactory.ACCUMULATOR accumulator,  int[] entries, TimeUnit timeUnit){
    switch (type){
      case BASIC:
        return new SimpleStats(name, entries[0], timeUnit, unitName);
      case FULL:
        return MovingAverageFactory.getInstance().createLinked(accumulator, name, entries, timeUnit, unitName);
      case NONE:
      default:
        return new NoStats(name, unitName);
    }
  }
}
