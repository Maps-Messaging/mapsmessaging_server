/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.aggregator.worker;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class AggregatorStripe {

  private final CopyOnWriteArrayList<SchedulableWorkItem> workItems;
  private final ConcurrentLinkedQueue<SchedulableWorkItem> readyQueue;

  public AggregatorStripe() {
    this.workItems = new CopyOnWriteArrayList<>();
    this.readyQueue = new ConcurrentLinkedQueue<>();
  }

  public void add(SchedulableWorkItem workItem) {
    workItems.add(workItem);
  }

  public void remove(SchedulableWorkItem workItem) {
    workItems.remove(workItem);
    // no attempt to purge from readyQueue; scheduled flag prevents harm
  }

  public void signal(SchedulableWorkItem workItem) {
    if (workItem.tryMarkScheduled()) {
      readyQueue.offer(workItem);
    }
  }

  public SchedulableWorkItem pollReady() {
    return readyQueue.poll();
  }
}
