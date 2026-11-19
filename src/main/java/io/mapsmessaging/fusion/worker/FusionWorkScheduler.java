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

import java.util.Objects;

public class FusionWorkScheduler {

  private static final String WORK_ITEM_REQUIRED = "workItem must not be null";


  private final FusionStripe[] stripes;
  private final FusionStripeWorker[] workers;
  private final Thread[] threads;

  public FusionWorkScheduler(int stripeCount, int maxBatchPerWorkItem, int idleSleepMs) {
    int resolvedStripeCount = resolveStripeCount(stripeCount);

    this.stripes = new FusionStripe[resolvedStripeCount];
    this.workers = new FusionStripeWorker[resolvedStripeCount];
    this.threads = new Thread[resolvedStripeCount];

    for (int index = 0; index < resolvedStripeCount; index++) {
      FusionStripe stripe = new FusionStripe();
      stripes[index] = stripe;

      FusionStripeWorker worker = new FusionStripeWorker(stripe, maxBatchPerWorkItem, idleSleepMs);
      workers[index] = worker;

      Thread thread = new Thread(worker, "FusionStripe-" + index);
      thread.setDaemon(true);
      threads[index] = thread;
    }
  }

  public void start() {
    for (Thread thread : threads) {
      thread.start();
    }
  }

  public void stop() {
    for (FusionStripeWorker worker : workers) {
      worker.shutdown();
    }
    for (Thread thread : threads) {
      try {
        thread.join(2000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void register(FusionWorkItem workItem) {
    Objects.requireNonNull(workItem, WORK_ITEM_REQUIRED);
    stripes[computeStripe(workItem.getName())].add(workItem);
  }

  public void unregister(FusionWorkItem workItem) {
    Objects.requireNonNull(workItem, WORK_ITEM_REQUIRED);
    stripes[computeStripe(workItem.getName())].remove(workItem);
  }

  public void signal(FusionWorkItem workItem) {
    Objects.requireNonNull(workItem, WORK_ITEM_REQUIRED);
    stripes[computeStripe(workItem.getName())].signal(workItem);
  }

  private int computeStripe(String name) {
    int hash = name.hashCode();
    int positive = hash & 0x7fffffff;
    return positive % stripes.length;
  }

  private int resolveStripeCount(int stripeCount) {
    if (stripeCount > 0) {
      return stripeCount;
    }

    int cpu = Runtime.getRuntime().availableProcessors();
    return Math.max(1, cpu / 2);
  }
}
