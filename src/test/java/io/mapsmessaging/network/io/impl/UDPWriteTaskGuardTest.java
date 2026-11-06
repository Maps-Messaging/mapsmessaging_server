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

package io.mapsmessaging.network.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MqttSn2OutboundPacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Verifies the UDP transport's defer/failure contract used by MQTT-SN renewal. */
class UDPWriteTaskGuardTest {

  @Test
  void deferred_datagram_does_not_write_or_complete_and_does_not_block_auth() throws Exception {
    EndPoint endpoint = mock(EndPoint.class);
    UDPWriteTask writer = writer(endpoint);
    AtomicInteger completions = new AtomicInteger();
    AtomicInteger deferrals = new AtomicInteger();
    writer.outboundFrame.add(new MqttSn2OutboundPacket(ByteBuffer.wrap(new byte[] {4, 13, 0, 1}),
        new InetSocketAddress("127.0.0.1", 1884), completions::incrementAndGet,
        (target, packet, written) -> {
          deferrals.incrementAndGet();
          return false;
        }, failure -> {}));
    writer.outboundFrame.add(new MqttSn2OutboundPacket(ByteBuffer.wrap(new byte[] {4, 13, 0, 2}),
        new InetSocketAddress("127.0.0.1", 1884), completions::incrementAndGet));
    writer.handleWrite();
    assertEquals(1, deferrals.get());
    assertEquals(0, completions.get());
    verify(endpoint, never()).sendPacket(any(Packet.class));
    writer.handleWrite();
    verify(endpoint).sendPacket(any(Packet.class));
    assertEquals(1, completions.get());
  }

  @Test
  void failed_datagram_notifies_cleanup_without_reporting_success() throws Exception {
    EndPoint endpoint = mock(EndPoint.class);
    IOException failure = new IOException("UDP write failed");
    doThrow(failure).when(endpoint).sendPacket(any(Packet.class));
    UDPWriteTask writer = writer(endpoint);
    AtomicInteger completions = new AtomicInteger();
    AtomicReference<IOException> observed = new AtomicReference<>();
    writer.outboundFrame.add(new MqttSn2OutboundPacket(ByteBuffer.wrap(new byte[] {4, 14, 4, (byte) 0x87}),
        new InetSocketAddress("127.0.0.1", 1884), completions::incrementAndGet, null, observed::set));
    writer.handleWrite();
    assertSame(failure, observed.get());
    assertEquals(0, completions.get());
  }

  private static UDPWriteTask writer(EndPoint endpoint) {
    SelectorCallback callback = mock(SelectorCallback.class);
    when(callback.getEndPoint()).thenReturn(endpoint);
    return new UDPWriteTask(callback, 1024, mock(SelectorTask.class), mock(Logger.class));
  }
}
