/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;

/** Owned by the protocol's authentication monitor; preserves submission order across both deferral points. */
final class MqttSn2PausedWrites {
  record Pending(long sequence, ByteBuffer wire, Runnable completion) {}

  private final PriorityQueue<Pending> packets = new PriorityQueue<>(Comparator.comparingLong(Pending::sequence));
  private long sequence;

  long nextSequence() {
    return ++sequence;
  }

  void defer(long submissionSequence, ByteBuffer wire, Runnable completion) {
    ByteBuffer copy = ByteBuffer.allocate(wire.remaining());
    copy.put(wire.asReadOnlyBuffer()).flip();
    packets.add(new Pending(submissionSequence, copy.asReadOnlyBuffer(), completion));
  }

  Pending poll() {
    return packets.poll();
  }

  boolean isEmpty() {
    return packets.isEmpty();
  }

  void clear() {
    packets.clear();
  }
}
