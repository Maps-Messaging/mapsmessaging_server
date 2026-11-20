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

package io.mapsmessaging.fusion.worker;

import io.mapsmessaging.fusion.FusionEnvelope;
import io.mapsmessaging.fusion.ProcessedHandler;
import io.mapsmessaging.fusion.StreamHandler;
import io.mapsmessaging.fusion.mailbox.QueueBackedMpscMailbox;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.fusion.FusionContributionMode;
import io.mapsmessaging.dto.rest.config.fusion.FusionInputConfigDTO;
import io.mapsmessaging.dto.rest.config.fusion.WindowCloseMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FusionWorkerTest {

  @Test
  void allInputs_firstAndLastModes_publishExpectedContributionsAndRunCompletions() {
    QueueBackedMpscMailbox<FusionEnvelope> mailbox = new QueueBackedMpscMailbox<>(10);
    CapturingProcessedHandler processedHandler = new CapturingProcessedHandler();
    FusionWorker worker = worker(
        mailbox,
        processedHandler,
        WindowCloseMode.ALL_INPUTS,
        handler(FusionContributionMode.FIRST),
        handler(FusionContributionMode.LAST)
    );
    AtomicInteger completions = new AtomicInteger();
    MessageEvent firstForFirstInput = event("first-1", completions::incrementAndGet);
    MessageEvent ignoredForFirstInput = event("first-2", completions::incrementAndGet);
    MessageEvent firstForLastInput = event("last-1", completions::incrementAndGet);
    MessageEvent replacementForLastInput = event("last-2", completions::incrementAndGet);

    mailbox.offer(new FusionEnvelope(0, firstForFirstInput));
    mailbox.offer(new FusionEnvelope(0, ignoredForFirstInput));
    mailbox.offer(new FusionEnvelope(1, firstForLastInput));
    mailbox.offer(new FusionEnvelope(1, replacementForLastInput));

    assertEquals(4, worker.drainOnce(10));

    assertEquals(1, processedHandler.windows.size());
    assertSame(firstForFirstInput, processedHandler.windows.get(0)[0]);
    assertSame(firstForLastInput, processedHandler.windows.get(0)[1]);
    assertEquals(4, completions.get());
  }

  @Test
  void timeoutOnly_zeroTimeout_publishesPartialWindowAndResetsForNextWindow() {
    QueueBackedMpscMailbox<FusionEnvelope> mailbox = new QueueBackedMpscMailbox<>(10);
    CapturingProcessedHandler processedHandler = new CapturingProcessedHandler();
    FusionWorker worker = worker(
        mailbox,
        processedHandler,
        WindowCloseMode.TIMEOUT_ONLY,
        handler(FusionContributionMode.LAST),
        handler(FusionContributionMode.LAST)
    );
    MessageEvent firstWindowEvent = event("first-window", null);
    MessageEvent secondWindowEvent = event("second-window", null);

    mailbox.offer(new FusionEnvelope(0, firstWindowEvent));
    worker.drainOnce(10);
    worker.checkTimeout();

    mailbox.offer(new FusionEnvelope(1, secondWindowEvent));
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
    QueueBackedMpscMailbox<FusionEnvelope> mailbox = new QueueBackedMpscMailbox<>(1);
    CapturingProcessedHandler processedHandler = new CapturingProcessedHandler();
    FusionWorker worker = worker(
        mailbox,
        processedHandler,
        WindowCloseMode.ALL_INPUTS,
        handler(FusionContributionMode.LAST)
    );
    MessageEvent event = event("input", () -> {
      throw new IllegalStateException("completion failed");
    });
    mailbox.offer(new FusionEnvelope(0, event));

    assertDoesNotThrow(() -> worker.drainOnce(1));
    assertEquals(1, processedHandler.windows.size());
    assertSame(event, processedHandler.windows.get(0)[0]);
  }

  @Test
  void scheduledFlag_allowsOnlyOneOutstandingSignalUntilCleared() {
    FusionWorker worker = worker(
        new QueueBackedMpscMailbox<>(1),
        contributions -> {
        },
        WindowCloseMode.TIMEOUT_ONLY,
        handler(FusionContributionMode.LAST)
    );

    assertTrue(worker.tryMarkScheduled());
    assertFalse(worker.tryMarkScheduled());

    worker.clearScheduled();

    assertTrue(worker.tryMarkScheduled());
  }

  private FusionWorker worker(
      QueueBackedMpscMailbox<FusionEnvelope> mailbox,
      ProcessedHandler processedHandler,
      WindowCloseMode closeMode,
      StreamHandler... handlers
  ) {
    return new FusionWorker("worker", mailbox, handlers, 0, processedHandler, closeMode, false);
  }

  private StreamHandler handler(FusionContributionMode mode) {
    FusionInputConfigDTO config = new FusionInputConfigDTO();
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
