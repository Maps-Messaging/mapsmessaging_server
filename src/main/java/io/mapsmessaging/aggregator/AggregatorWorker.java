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

package io.mapsmessaging.aggregator;

import io.mapsmessaging.aggregator.mailbox.AggregatorMailbox;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorContributionMode;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class AggregatorWorker implements Runnable {

  private final AggregatorMailbox<AggregatorEnvelope> mailbox;
  private final StreamHandler[] handlers;
  private final long timeoutMillis;

  private final Message[] contributions;
  private final boolean[] seen;

  private volatile boolean running;
  private long deadlineMillis;

  public AggregatorWorker(
      AggregatorMailbox<AggregatorEnvelope> mailbox,
      StreamHandler[] handlers,
      long timeoutMillis
  ) {
    this.mailbox = mailbox;
    this.handlers = handlers;
    this.timeoutMillis = timeoutMillis;

    this.contributions = new Message[handlers.length];
    this.seen = new boolean[handlers.length];

    this.running = true;
    this.deadlineMillis = -1;
  }

  public void shutdown() {
    this.running = false;
  }

  @Override
  public void run() {
    while (running) {
      int drained = mailbox.drainTo(this::processEnvelope, 1024);

      long now = System.currentTimeMillis();
      if (deadlineMillis > 0 && now >= deadlineMillis) {
        if (hasAnyContribution()) {
          publishAndReset();
        }
        deadlineMillis = -1;
      }

      if (drained == 0) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
      }
    }
  }

  private void processEnvelope(AggregatorEnvelope envelope) {
    if (deadlineMillis < 0) {
      deadlineMillis = System.currentTimeMillis() + timeoutMillis;
    }

    int index = envelope.getInputIndex();
    MessageEvent event = envelope.getEvent();

    Message processed = handlers[index].process(event);
    if (processed != null) {
      applyContribution(index, processed, handlers[index].getContributionMode());
    }

    runCompletion(event);

    if (allSeen()) {
      publishAndReset();
      deadlineMillis = -1;
    }
  }

  private void applyContribution(int index, Message message, AggregatorContributionMode mode) {
    if (mode == AggregatorContributionMode.FIRST) {
      if (!seen[index]) {
        contributions[index] = message;
        seen[index] = true;
      }
      return;
    }

    contributions[index] = message;
    seen[index] = true;
  }

  private boolean allSeen() {
    for (boolean value : seen) {
      if (!value) {
        return false;
      }
    }
    return true;
  }

  private boolean hasAnyContribution() {
    for (boolean value : seen) {
      if (value) {
        return true;
      }
    }
    return false;
  }

  private void publishAndReset() {

    // TODO aggregate contributions[] and publish to output topic.
    // Snapshot first if publish can re-enter or async.
    for (int i = 0; i < contributions.length; i++) {
      contributions[i] = null;
      seen[i] = false;
    }
  }

  private void runCompletion(MessageEvent event) {
    Runnable completionTask = event.getCompletionTask();
    if (completionTask != null) {
      try {
        completionTask.run();
      } catch (Throwable ignored) {
        // Completion must not stop the worker.
      }
    }
  }
}
