/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
