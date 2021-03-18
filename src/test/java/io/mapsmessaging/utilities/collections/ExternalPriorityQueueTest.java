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

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ExternalPriorityQueueTest extends PriorityQueueTest{

  @Override
  public PriorityQueue<TestData> createQueue(int priorities) {
    Queue<TestData>[] external = new Queue[priorities];
    for(int x=0;x<priorities;x++){
      external[x] = new LinkedBlockingQueue<>();
    }
    TestDataPriorityFactory factory = new TestDataPriorityFactory();
    return new PriorityQueue<>(external,factory);
  }
}
