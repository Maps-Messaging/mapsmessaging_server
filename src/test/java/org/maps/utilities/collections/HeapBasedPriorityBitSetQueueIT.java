package org.maps.utilities.collections;

import java.util.Queue;
import org.maps.utilities.collections.bitset.ByteBufferBitSetFactoryImpl;

public class HeapBasedPriorityBitSetQueueIT extends PriorityBitSetQueueIT {
  public PriorityQueue<Long> createQueue(int priorities){
    Queue<Long>[] external = new Queue[priorities];
    for(int x=0;x<priorities;x++){
      external[x] = new NaturalOrderedLongQueue(x, new ByteBufferBitSetFactoryImpl(4096));
    }
    return new PriorityQueue<>(external,null);
  }

}
