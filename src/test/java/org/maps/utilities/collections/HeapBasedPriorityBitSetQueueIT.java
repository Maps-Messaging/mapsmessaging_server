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
