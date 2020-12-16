package org.maps.utilities.collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.BaseTest;

public abstract class PriorityBitSetQueueIT extends BaseTest {

  private static final int RUN_TIME = 1000;

  public abstract PriorityQueue<Long> createQueue(int priorities);


  @Test
  public void testLinearPerformanceEntries() throws Exception{
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
