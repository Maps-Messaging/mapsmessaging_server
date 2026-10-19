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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MqttSn2PausedWritesTest {
  @Test
  void backlogged_older_packet_precedes_newer_packet_deferred_during_renewal() {
    MqttSn2PausedWrites writes = new MqttSn2PausedWrites();
    long older = writes.nextSequence();
    long newer = writes.nextSequence();
    Runnable completion = new AtomicInteger()::incrementAndGet;
    // The newer packet reaches the submission gate first. The older one reaches
    // the actual UDP write gate later, after already waiting in the selector.
    writes.defer(newer, ByteBuffer.wrap(new byte[] {2}), null);
    writes.defer(older, ByteBuffer.wrap(new byte[] {1}), completion);
    MqttSn2PausedWrites.Pending first = writes.poll();
    assertEquals(1, first.wire().get());
    assertSame(completion, first.completion());
    assertEquals(2, writes.poll().wire().get());
    assertTrue(writes.isEmpty());
  }

  @Test
  void deferred_bytes_survive_source_reuse_and_close_releases_pending_writes() {
    MqttSn2PausedWrites writes = new MqttSn2PausedWrites();
    ByteBuffer source = ByteBuffer.wrap(new byte[] {1, 2});
    writes.defer(writes.nextSequence(), source, null);
    source.put(0, (byte) 9);
    assertEquals(1, writes.poll().wire().get());
    writes.defer(writes.nextSequence(), source, null);
    writes.clear();
    assertTrue(writes.isEmpty());
  }
}
