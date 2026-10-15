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

package io.mapsmessaging.state.adapter.cot;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.metrics.MessageOutcomeStats;
import io.mapsmessaging.utilities.admin.JMXManager;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CotIngestAdapterDeliveryFailureTest {

  private boolean originalJmx;

  @BeforeEach
  void disableJmx() {
    originalJmx = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
  }

  @AfterEach
  void restoreJmx() {
    JMXManager.setEnableJMX(originalJmx);
  }

  @ParameterizedTest
  @EnumSource(FailurePoint.class)
  void delivery_failure_is_counted_completed_and_does_not_prevent_the_next_delivery(FailurePoint failurePoint) {
    TwinManager twins = new TwinManager();
    AtomicBoolean rejectEnqueue = new AtomicBoolean(failurePoint == FailurePoint.ENQUEUE);
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twins) {
      @Override
      void enqueue(String destinationName, byte[] payload) {
        if (rejectEnqueue.getAndSet(false)) {
          throw new IllegalStateException("Rejected enqueue");
        }
        super.enqueue(destinationName, payload);
      }
    };
    byte[] payload = ("""
        <event version="2.0" uid="delivery-recovery" type="a-f-A-M-F-Q" how="m-g"
          time="2026-10-15T00:00:00Z" start="2026-10-15T00:00:00Z" stale="2026-10-16T00:00:00Z">
          <point lat="38.0" lon="-9.0" hae="10" ce="1" le="1"/>
        </event>
        """).getBytes(StandardCharsets.UTF_8);
    String destination = "/tak/cot/inbound/edge-a";
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(message);
    when(message.getOpaqueData()).thenReturn(payload);
    when(event.getDestinationName()).thenReturn(destination);
    when(event.getCompletionTask()).thenReturn(completion);
    switch (failurePoint) {
      case MESSAGE -> when(event.getMessage()).thenThrow(new IllegalStateException("Message access")).thenReturn(message);
      case PAYLOAD -> when(message.getOpaqueData()).thenThrow(new IllegalStateException("Payload access")).thenReturn(payload);
      case DESTINATION -> when(event.getDestinationName()).thenThrow(new IllegalStateException("Destination access")).thenReturn(destination);
      case ENQUEUE -> {
        // The queue boundary fails once in the initialized adapter above.
      }
    }
    long failures = failureCount();

    assertDoesNotThrow(() -> adapter.sendMessage(event));

    verify(completion, times(1)).run();
    assertEquals(0, adapter.getQueueSize());
    assertEquals(failures + 1, failureCount());

    assertDoesNotThrow(() -> adapter.sendMessage(event));

    verify(completion, times(2)).run();
    assertEquals(1, adapter.getQueueSize());
    assertEquals(0, twins.getTwinCount(), "Delivery must only queue; twin processing belongs to the worker");
    assertEquals(failures + 1, failureCount());
  }

  @Test
  void delivery_failure_without_a_completion_callback_is_also_contained() {
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", new TwinManager());
    MessageEvent event = mock(MessageEvent.class);
    when(event.getMessage()).thenThrow(new IllegalStateException("Message access"));
    long failures = failureCount();

    assertDoesNotThrow(() -> adapter.sendMessage(event));

    assertEquals(0, adapter.getQueueSize());
    assertEquals(failures + 1, failureCount());
  }

  private static long failureCount() {
    return MessageOutcomeStats.getCount(MessageOutcomeStats.Source.COT_INGEST, "unparseable", MessageOutcomeStats.Category.FAILURE);
  }

  private enum FailurePoint {
    MESSAGE,
    PAYLOAD,
    DESTINATION,
    ENQUEUE
  }
}
