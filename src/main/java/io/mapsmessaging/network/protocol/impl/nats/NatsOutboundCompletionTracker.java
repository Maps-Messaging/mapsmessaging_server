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

import io.mapsmessaging.network.protocol.impl.nats.frames.CompletionHandler;
import io.mapsmessaging.network.protocol.impl.nats.frames.HMsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks pending Core NATS network writes so closing a connection also releases
 * MAPS delivery completion tasks when the network cannot complete a frame.
 */
public class NatsOutboundCompletionTracker {

  private final Set<NatsFrame> pending = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean closing = new AtomicBoolean();

  public void track(NatsFrame frame) {
    synchronized (frame) {
      CompletionHandler callback = frame.getCallback();
      if (callback == null || (!(frame instanceof MsgFrame) && !(frame instanceof HMsgFrame))) {
        return;
      }
      if (closing.get()) {
        frame.complete();
        return;
      }
      frame.setCallback(() -> {
        pending.remove(frame);
        callback.run();
      });
      pending.add(frame);
    }
  }

  public void failOutstandingWrites() {
    closing.set(true);
    for (NatsFrame frame : Set.copyOf(pending)) {
      frame.complete();
    }
  }

  public int pendingCount() {
    return pending.size();
  }
}
