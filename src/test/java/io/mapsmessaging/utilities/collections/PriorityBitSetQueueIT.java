/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.BaseTest;


abstract class PriorityBitSetQueueIT extends BaseTest {

  private static final int RUN_TIME = 1000;

  public abstract PriorityQueue<Long> createQueue(int priorities);


  @Test
  void testLinearPerformanceEntries() throws Exception{
    try (PriorityQueue<Long> priorityQueue = createQueue(16)){
      long endTime = System.currentTimeMillis()+ RUN_TIME;
      long count = 0;

      while(endTime > System.currentTimeMillis() && count < 1000000000L){
        long value = count;
        int priority = ((int)count % 16);
        priorityQueue.add(value, priority);
        count++;
      }
      System.err.println("Added "+priorityQueue.size()+" in "+(RUN_TIME/1000)+" seconds");

      long pollTime = System.currentTimeMillis();
      Assertions.assertEquals(priorityQueue.size(), count);
      while(!priorityQueue.isEmpty()){
        long test1 = priorityQueue.poll();
      }
      System.err.println("Time to drain "+(System.currentTimeMillis() - pollTime));
    }
  }

}
