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
import io.mapsmessaging.aggregator.ProcessedHandler;
import io.mapsmessaging.aggregator.StreamHandler;
import io.mapsmessaging.aggregator.mailbox.QueueBackedMpscMailbox;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorContributionMode;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.WindowCloseMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AggregatorWorkerTest {

  @Test
  void allInputs_firstAndLastModes_publishExpectedContributionsAndRunCompletions() {
    QueueBackedMpscMailbox<AggregatorEnvelope> mailbox = new QueueBackedMpscMailbox<>(10);
    CapturingProcessedHandler processedHandler = new CapturingProcessedHandler();
    AggregatorWorker worker = worker(
        mailbox,
        processedHandler,
        WindowCloseMode.ALL_INPUTS,
        handler(AggregatorContributionMode.FIRST),
        handler(AggregatorContributionMode.LAST)
    );
    AtomicInteger completions = new AtomicInteger();
    MessageEvent firstForFirstInput = event("first-1", completions::incrementAndGet);
    MessageEvent ignoredForFirstInput = event("first-2", completions::incrementAndGet);
    MessageEvent firstForLastInput = event("last-1", completions::incrementAndGet);
    MessageEvent replacementForLastInput = event("last-2", completions::incrementAndGet);

    mailbox.offer(new AggregatorEnvelope(0, firstForFirstInput));
    mailbox.offer(new AggregatorEnvelope(0, ignoredForFirstInput));
    mailbox.offer(new AggregatorEnvelope(1, firstForLastInput));
    mailbox.offer(new AggregatorEnvelope(1, replacementForLastInput));

    assertEquals(4, worker.drainOnce(10));

    assertEquals(1, processedHandler.windows.size());
    assertSame(firstForFirstInput, processedHandler.windows.get(0)[0]);
    assertSame(firstForLastInput, processedHandler.windows.get(0)[1]);
    assertEquals(4, completions.get());
  }

  @Test
  void timeoutOnly_zeroTimeout_publishesPartialWindowAndResetsForNextWindow() {
    QueueBackedMpscMailbox<AggregatorEnvelope> mailbox = new QueueBackedMpscMailbox<>(10);
    CapturingProcessedHandler processedHandler = new CapturingProcessedHandler();
    AggregatorWorker worker = worker(
        mailbox,
        processedHandler,
        WindowCloseMode.TIMEOUT_ONLY,
        handler(AggregatorContributionMode.LAST),
        handler(AggregatorContributionMode.LAST)
    );
    MessageEvent firstWindowEvent = event("first-window", null);
    MessageEvent secondWindowEvent = event("second-window", null);

    mailbox.offer(new AggregatorEnvelope(0, firstWindowEvent));
    worker.drainOnce(10);
    worker.checkTimeout();

    mailbox.offer(new AggregatorEnvelope(1, secondWindowEvent));
    worker.drainOnce(10);
    worker.checkTimeout();

    assertEquals(2, processedHandler.windows.size());
    assertSame(firstWindowEvent, processedHandler.windows.get(0)[0]);
    assertNull(processedHandler.windows.get(0)[1]);
    assertNull(processedHandler.windows.get(1)[0]);
    assertSame(secondWindowEvent, processedHandler.windows.get(1)[1]);
  }

  @Test
  void completionFailure_doesNotPreventWindowPublication() {
    QueueBackedMpscMailbox<AggregatorEnvelope> mailbox = new QueueBackedMpscMailbox<>(1);
    CapturingProcessedHandler processedHandler = new CapturingProcessedHandler();
    AggregatorWorker worker = worker(
        mailbox,
        processedHandler,
        WindowCloseMode.ALL_INPUTS,
        handler(AggregatorContributionMode.LAST)
    );
    MessageEvent event = event("input", () -> {
      throw new IllegalStateException("completion failed");
    });
    mailbox.offer(new AggregatorEnvelope(0, event));

    assertDoesNotThrow(() -> worker.drainOnce(1));
    assertEquals(1, processedHandler.windows.size());
    assertSame(event, processedHandler.windows.get(0)[0]);
  }

  @Test
  void scheduledFlag_allowsOnlyOneOutstandingSignalUntilCleared() {
    AggregatorWorker worker = worker(
        new QueueBackedMpscMailbox<>(1),
        contributions -> {
        },
        WindowCloseMode.TIMEOUT_ONLY,
        handler(AggregatorContributionMode.LAST)
    );

    assertTrue(worker.tryMarkScheduled());
    assertFalse(worker.tryMarkScheduled());

    worker.clearScheduled();

    assertTrue(worker.tryMarkScheduled());
  }

  private AggregatorWorker worker(
      QueueBackedMpscMailbox<AggregatorEnvelope> mailbox,
      ProcessedHandler processedHandler,
      WindowCloseMode closeMode,
      StreamHandler... handlers
  ) {
    return new AggregatorWorker("worker", mailbox, handlers, 0, processedHandler, closeMode, false);
  }

  private StreamHandler handler(AggregatorContributionMode mode) {
    AggregatorInputConfigDTO config = new AggregatorInputConfigDTO();
    config.setTopicName("/input");
    config.setContributionMode(mode);
    return new StreamHandler(config);
  }

  private MessageEvent event(String destinationName, Runnable completionTask) {
    return new MessageEvent(destinationName, null, new MessageBuilder().build(), completionTask);
  }

  private static class CapturingProcessedHandler implements ProcessedHandler {

    private final List<MessageEvent[]> windows = new ArrayList<>();

    @Override
    public void completed(MessageEvent[] contributions) {
      windows.add(Arrays.copyOf(contributions, contributions.length));
    }
  }
}
