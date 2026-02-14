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

import io.mapsmessaging.aggregator.AggregatorEnvelope;
import io.mapsmessaging.aggregator.StreamHandler;
import io.mapsmessaging.aggregator.mailbox.AggregatorMailbox;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorContributionMode;

import java.util.concurrent.atomic.AtomicBoolean;

public class AggregatorWorker implements AggregatorWorkItem {
  private final AtomicBoolean scheduled;
  private final String name;
  private final AggregatorMailbox<AggregatorEnvelope> mailbox;
  private final StreamHandler[] handlers;

  private final long timeoutMillis;

  private final MessageEvent[] contributions;
  private final boolean[] seen;

  private long deadlineMillis;

  public AggregatorWorker(
      String name,
      AggregatorMailbox<AggregatorEnvelope> mailbox,
      StreamHandler[] handlers,
      long timeoutMillis
  ) {
    this.name = name;
    this.mailbox = mailbox;
    this.handlers = handlers;
    this.timeoutMillis = timeoutMillis;
    this.scheduled = new AtomicBoolean(false);

    this.contributions = new MessageEvent[handlers.length];
    this.seen = new boolean[handlers.length];

    this.deadlineMillis = -1;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int drainOnce(int maxBatch) {
    return mailbox.drainTo(this::processEnvelope, maxBatch);
  }

  @Override
  public boolean tryMarkScheduled() {
    return scheduled.compareAndSet(false, true);
  }

  @Override
  public void clearScheduled() {
    scheduled.set(false);
  }


  @Override
  public void checkTimeout() {
    if (deadlineMillis < 0) {
      return;
    }

    long now = System.currentTimeMillis();
    if (now >= deadlineMillis) {
      if (hasAnyContribution()) {
        publishAndReset(false);
      }
      deadlineMillis = -1;
    }
  }

  private void processEnvelope(AggregatorEnvelope envelope) {
    long now = System.currentTimeMillis();
    if (deadlineMillis < 0) {
      deadlineMillis = now + timeoutMillis;
    }

    int index = envelope.getInputIndex();
    MessageEvent event = envelope.getEvent();

    MessageEvent processed = handlers[index].process(event);
    if (processed != null ) {
      applyContribution(index, processed, handlers[index].getContributionMode());
    }
    runCompletion(event);
    if (allSeen()) {
      publishAndReset(true);
      deadlineMillis = -1;
    }
  }

  private void applyContribution(int index, MessageEvent message, AggregatorContributionMode mode) {
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

  private void publishAndReset(boolean closedByAllInputs) {
    // TODO Phase 1: build output message using contributions[] and publish
    // 'closedByAllInputs' indicates ALL_INPUTS vs TIMEOUT.
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
        // Completion must not stop progress.
      }
    }
  }
}
