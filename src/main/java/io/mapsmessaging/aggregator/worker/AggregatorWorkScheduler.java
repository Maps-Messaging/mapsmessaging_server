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

public class AggregatorWorkScheduler {

  private final AggregatorStripe[] stripes;
  private final AggregatorStripeWorker[] workers;
  private final Thread[] threads;

  public AggregatorWorkScheduler(int stripeCount, int maxBatchPerWorkItem, int idleSleepMs) {
    int resolvedStripeCount = resolveStripeCount(stripeCount);

    this.stripes = new AggregatorStripe[resolvedStripeCount];
    this.workers = new AggregatorStripeWorker[resolvedStripeCount];
    this.threads = new Thread[resolvedStripeCount];

    for (int index = 0; index < resolvedStripeCount; index++) {
      AggregatorStripe stripe = new AggregatorStripe();
      stripes[index] = stripe;

      AggregatorStripeWorker worker = new AggregatorStripeWorker(stripe, maxBatchPerWorkItem, idleSleepMs);
      workers[index] = worker;

      Thread thread = new Thread(worker, "AggregatorStripe-" + index);
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
    for (AggregatorStripeWorker worker : workers) {
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

  public void register(AggregatorWorkItem workItem) {
    Objects.requireNonNull(workItem, "workItem must not be null");
    stripes[computeStripe(workItem.getName())].add(workItem);
  }

  public void unregister(AggregatorWorkItem workItem) {
    Objects.requireNonNull(workItem, "workItem must not be null");
    stripes[computeStripe(workItem.getName())].remove(workItem);
  }

  public void signal(AggregatorWorkItem workItem) {
    Objects.requireNonNull(workItem, "workItem must not be null");
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
    int resolved = Math.max(1, cpu / 2);
    return resolved;
  }
}
