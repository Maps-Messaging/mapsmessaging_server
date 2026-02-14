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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class AggregatorStripeWorker implements Runnable {

  private final AggregatorStripe stripe;
  private final int maxBatchPerWorkItem;
  private final int idleSleepMs;

  private volatile boolean running;

  public AggregatorStripeWorker(AggregatorStripe stripe, int maxBatchPerWorkItem, int idleSleepMs) {
    this.stripe = stripe;
    this.maxBatchPerWorkItem = maxBatchPerWorkItem;
    this.idleSleepMs = idleSleepMs;
    this.running = true;
  }

  public void shutdown() {
    this.running = false;
  }

  @Override
  public void run() {
    while (running) {
      boolean didWork = false;

      SchedulableWorkItem workItem = stripe.pollReady();
      while (workItem != null) {
        workItem.clearScheduled();

        int drained = workItem.drainOnce(maxBatchPerWorkItem);
        workItem.checkTimeout();

        if (drained > 0) {
          didWork = true;

          // If there is still more work queued, re-signal to be fair but keep progress.
          if (drained == maxBatchPerWorkItem) {
            stripe.signal(workItem);
          }
        }

        workItem = stripe.pollReady();
      }

      if (!didWork) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(idleSleepMs));
      }
    }
  }
}
