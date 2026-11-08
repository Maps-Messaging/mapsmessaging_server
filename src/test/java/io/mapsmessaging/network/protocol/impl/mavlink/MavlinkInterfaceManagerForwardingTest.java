/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

class MavlinkInterfaceManagerForwardingTest {

  @Test
  void forwardsOriginalBytesToEachConfiguredDestination() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    when(endPoint.sendPacket(any(Packet.class))).thenReturn(4);
    InetSocketAddress first = new InetSocketAddress("127.0.0.2", 14550);
    InetSocketAddress second = new InetSocketAddress("127.0.0.3", 14551);
    MavlinkInterfaceManager manager = fixture(endPoint, List.of(first, second));
    byte[] raw = new byte[]{1, 2, 3, 4};

    invokeForward(manager, raw, false);

    ArgumentCaptor<Packet> sent = ArgumentCaptor.forClass(Packet.class);
    verify(endPoint, org.mockito.Mockito.times(2)).sendPacket(sent.capture());
    assertEquals(first, sent.getAllValues().get(0).getFromAddress());
    assertEquals(second, sent.getAllValues().get(1).getFromAddress());
    for (Packet packet : sent.getAllValues()) {
      byte[] encoded = new byte[packet.available()];
      packet.get(encoded);
      assertArrayEquals(raw, encoded);
    }
  }

  @Test
  void forwardedSourceIsNotForwardedAgain() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    MavlinkInterfaceManager manager = fixture(endPoint, List.of(new InetSocketAddress("127.0.0.2", 14550)));

    invokeForward(manager, new byte[]{1, 2}, true);

    verify(endPoint, never()).sendPacket(any(Packet.class));
  }

  @Test
  void oneForwardDestinationFailureDoesNotBlockRemainingDestinations() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    doThrow(new IOException("simulated forwarding failure")).doReturn(2).when(endPoint).sendPacket(any(Packet.class));
    MavlinkInterfaceManager manager = fixture(endPoint,
        List.of(new InetSocketAddress("127.0.0.2", 14550), new InetSocketAddress("127.0.0.3", 14550)));

    assertDoesNotThrow(() -> invokeForward(manager, new byte[]{1, 2}, false));
    verify(endPoint, org.mockito.Mockito.times(2)).sendPacket(any(Packet.class));
  }

  private static MavlinkInterfaceManager fixture(EndPoint endPoint, List<InetSocketAddress> destinations) throws Exception {
    MavlinkInterfaceManager manager = mock(MavlinkInterfaceManager.class, Answers.CALLS_REAL_METHODS);
    Field endpointField = MavlinkInterfaceManager.class.getDeclaredField("endPoint");
    endpointField.setAccessible(true);
    endpointField.set(manager, endPoint);
    Field forwardsField = MavlinkInterfaceManager.class.getDeclaredField("forwardList");
    forwardsField.setAccessible(true);
    forwardsField.set(manager, destinations);
    return manager;
  }

  private static void invokeForward(MavlinkInterfaceManager manager, byte[] raw, boolean forwardedSource) throws Exception {
    Method method = MavlinkInterfaceManager.class.getDeclaredMethod("forwardUnlessForwarded", byte[].class, boolean.class);
    method.setAccessible(true);
    method.invoke(manager, raw, forwardedSource);
  }
}
