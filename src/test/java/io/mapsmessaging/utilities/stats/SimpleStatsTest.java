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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleStatsTest {


  @Test
  void validateFunctions() throws InterruptedException {
    Stats stats = new SimpleStats( "test", 10, TimeUnit.SECONDS, "ticks");
    runTests(stats);
  }

  @Test
  void validateComplexFunctions() throws InterruptedException {
    Stats stats = MovingAverageFactory.getInstance().createLinked(MovingAverageFactory.ACCUMULATOR.ADD, "test", 9, 10, 4, TimeUnit.SECONDS, "Ticks");
    runTests(stats);
  }
  private void runTests(Stats stats) throws InterruptedException {
    long count =0;
    for(int x=0;x<30;x++){
      stats.add(1);
      Thread.sleep(1000);
      stats.update();
      count++;
      if(count > 9){
        Assertions.assertEquals(1, Math.round(stats.getPerSecond()));
      }
      else{
        Assertions.assertEquals(0, (int)stats.getPerSecond());
      }
      Assertions.assertEquals(count, stats.getTotal());
      Assertions.assertEquals(1, (int)stats.getCurrent());
    }
    stats.reset();
    count =0;
    for(int x=0;x<30;x++){
      stats.add(2);
      Thread.sleep(1000);
      stats.update();
      count++;
      if(count > 9){
        Assertions.assertEquals(2.0f, Math.round(stats.getPerSecond()));
      }
      else{
        Assertions.assertEquals(0, (int)stats.getPerSecond());
      }
      Assertions.assertEquals(count*2, stats.getTotal());
      Assertions.assertEquals(2, stats.getCurrent());
    }
  }
}
