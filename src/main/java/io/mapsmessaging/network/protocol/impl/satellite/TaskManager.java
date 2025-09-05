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

package io.mapsmessaging.network.protocol.impl.satellite;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager {
  private static final AtomicInteger count = new AtomicInteger(1);

  private final ExecutorService existingPool;
  private final ScheduledExecutorService scheduler;

  public TaskManager() {
    this(4);
  }

  public TaskManager(int poolSize) {
    existingPool= Executors.newFixedThreadPool(4, r -> new Thread(r, "Satellite-Executors-" + count.getAndIncrement()));
    scheduler = new ScheduledThreadPoolExecutor(poolSize, ((ThreadPoolExecutor) existingPool).getThreadFactory());
  }

  public void close(){
    scheduler.shutdown();
    existingPool.shutdown();
  }

  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
    return scheduler.schedule(task, delay, unit);
  }

}
