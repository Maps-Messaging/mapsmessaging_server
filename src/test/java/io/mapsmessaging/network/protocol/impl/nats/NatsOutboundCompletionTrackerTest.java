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

package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NatsOutboundCompletionTrackerTest {

  @Test
  void successfulWriteCompletesAndRemovesPendingFrameExactlyOnce() {
    NatsOutboundCompletionTracker tracker = new NatsOutboundCompletionTracker();
    MsgFrame frame = new MsgFrame(128);
    AtomicInteger deliveries = new AtomicInteger();
    frame.setCallback(deliveries::incrementAndGet);

    tracker.track(frame);
    assertEquals(1, tracker.pendingCount());
    frame.complete();
    tracker.failOutstandingWrites();

    assertEquals(0, tracker.pendingCount());
    assertEquals(1, deliveries.get());
  }

  @Test
  void interruptedNetworkWritesReleaseDeliveryOnSessionShutdown() {
    NatsOutboundCompletionTracker tracker = new NatsOutboundCompletionTracker();
    MsgFrame frame = new MsgFrame(128);
    AtomicInteger deliveries = new AtomicInteger();
    frame.setCallback(deliveries::incrementAndGet);

    tracker.track(frame);
    tracker.failOutstandingWrites();
    frame.complete();
    tracker.failOutstandingWrites();

    assertEquals(1, deliveries.get());
    assertEquals(0, tracker.pendingCount());
  }

  @Test
  void framesWithoutMapsDeliveryCallbacksAreNotTracked() {
    NatsOutboundCompletionTracker tracker = new NatsOutboundCompletionTracker();
    MsgFrame frame = new MsgFrame(128);

    tracker.track(frame);
    tracker.failOutstandingWrites();

    assertEquals(0, tracker.pendingCount());
  }
  @Test
  void writesSubmittedAfterCloseCannotLeaveUnresolvedCompletions() {
    NatsOutboundCompletionTracker tracker = new NatsOutboundCompletionTracker();
    tracker.failOutstandingWrites();
    MsgFrame frame = new MsgFrame(128);
    AtomicInteger deliveries = new AtomicInteger();
    frame.setCallback(deliveries::incrementAndGet);

    tracker.track(frame);

    assertEquals(1, deliveries.get());
    assertEquals(0, tracker.pendingCount());
  }
}
