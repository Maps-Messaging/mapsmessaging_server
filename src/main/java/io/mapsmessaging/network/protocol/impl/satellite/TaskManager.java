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

import io.mapsmessaging.auth.AuthManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@SuppressWarnings("java:S6548") // yes it is a singleton
public class TaskManager {

  private static class Holder {
    static final TaskManager INSTANCE = new TaskManager();
  }
  public static TaskManager getInstance() {
    return Holder.INSTANCE;
  }

  private static final AtomicInteger count = new AtomicInteger(1);

  private final ExecutorService existingPool;
  private final ScheduledExecutorService scheduler;

  private TaskManager() {
    this(4);
  }

  private TaskManager(int poolSize) {
    existingPool= Executors.newFixedThreadPool(4, r -> new Thread(r, "Satellite-Executors-" + count.getAndIncrement()));
    scheduler = new ScheduledThreadPoolExecutor(poolSize, ((ThreadPoolExecutor) existingPool).getThreadFactory());
  }

  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
    return scheduler.schedule(task, delay, unit);
  }

}
