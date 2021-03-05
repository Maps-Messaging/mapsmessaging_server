/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package org.maps.utilities.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.maps.utilities.stats.MovingAverageFactory.ACCUMULATOR;

class LinkedMovingAveragesTest {

  @Test
  void basicFunctions(){
    // We have create a list of moving average buckets starting with 1 second, incrementing by 10 seconds for a total of 6 buckets
    LinkedMovingAverages linked = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Test", 1, 10, 6,  TimeUnit.SECONDS, "Tests");
    long epoch = System.currentTimeMillis() + 500;
    long count=0;
    while(epoch > System.currentTimeMillis()){
      linked.add(1);
      count++;
    }
    linked.update();
    int div = 1;
    for(MovingAverage movingAverage:linked.movingAverages){
      assertEquals(count/div, (movingAverage.getAverage()));
      if(div == 1)div = 0;
      div += 10;
    }

  }
}