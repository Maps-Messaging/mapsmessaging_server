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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MovingAverageTest {

  private static final long EPOCH = 100;

  @Test
  void getName() {
    MovingAverage movingAverage = new MovingAverage(1, TimeUnit.SECONDS);
    Assertions.assertEquals(1+"_"+TimeUnit.SECONDS.toString(), movingAverage.getName());
  }

  @Test
  void add() {
    MovingAverage movingAverage = new MovingAverage(1, TimeUnit.SECONDS);
    long count =0;
    long epoch = System.currentTimeMillis() + EPOCH;
    while(epoch > System.currentTimeMillis()){
      movingAverage.add(1);
      count++;
    }
    movingAverage.update();
    Assertions.assertEquals(count, movingAverage.getAverage());
  }

  @Test
  void reset() {
    MovingAverage movingAverage = new MovingAverage(1, TimeUnit.SECONDS);
    long epoch = System.currentTimeMillis() + EPOCH;
    while(epoch > System.currentTimeMillis()){
      movingAverage.add(1);
    }
    movingAverage.reset();
    Assertions.assertEquals(0, movingAverage.getAverage());
  }

  @Test
  void clearData() {
    MovingAverage movingAverage = new MovingAverage(1, TimeUnit.SECONDS);
    long epoch = System.currentTimeMillis() + EPOCH;
    while(epoch > System.currentTimeMillis()){
      movingAverage.add(1);
    }
    epoch = System.currentTimeMillis() + 1100;
    while(epoch > System.currentTimeMillis()){
      LockSupport.parkNanos(1000000000);
    }
    // Should be cleared since it has exceeded the time unit
    Assertions.assertEquals(0, movingAverage.getAverage());
  }
}
