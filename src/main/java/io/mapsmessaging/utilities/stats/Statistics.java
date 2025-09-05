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

import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Statistics {

  private final List<Stats> averageList;

  public Statistics() {
    averageList = new ArrayList<>();
  }

  public List<Stats> getAverageList() {
    return new ArrayList<>(averageList);
  }

  public Stats create(StatsType full, ACCUMULATOR accumulator, String name, String units) {
    Stats stats = StatsFactory.create(full, name, units, accumulator,1, 5, 4, TimeUnit.MINUTES);
    averageList.add(stats);
    return stats;
  }

}
