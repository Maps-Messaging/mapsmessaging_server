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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

class MavLinkSessionManagerTest {

  @Test
  void activeSessionRemainsRegisteredDuringExpiryScan() throws IOException {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable context = mock(Timeoutable.class);
    MavlinkDeviceKey key = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    manager.addState(key, new UDPSessionState<>(context));

    manager.scanForTimeouts();

    assertNotNull(manager.getState(key));
    verify(context, never()).close();
    manager.close();
    verify(context).close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void idleSessionExpiresWithoutAContextOverride() throws IOException {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable context = mock(Timeoutable.class);
    UDPSessionState<Timeoutable> state = mock(UDPSessionState.class);
    when(state.getContext()).thenReturn(context);
    when(state.getGetLastAccess()).thenReturn(0L);
    MavlinkDeviceKey key = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    manager.addState(key, state);

    manager.scanForTimeouts();

    assertNull(manager.getState(key));
    verify(context).close();
    manager.close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void contextSpecificTimeoutOverridesManagerDefault() throws IOException {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(1);
    Timeoutable context = mock(Timeoutable.class);
    when(context.getTimeOut()).thenReturn(java.util.concurrent.TimeUnit.DAYS.toMillis(1));
    UDPSessionState<Timeoutable> state = mock(UDPSessionState.class);
    when(state.getContext()).thenReturn(context);
    when(state.getGetLastAccess()).thenReturn(System.currentTimeMillis() - java.util.concurrent.TimeUnit.HOURS.toMillis(1));
    MavlinkDeviceKey key = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    manager.addState(key, state);

    manager.scanForTimeouts();

    assertNotNull(manager.getState(key));
    verify(context, never()).close();
    manager.close();
    verify(context).close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void expiredVehicleDoesNotCloseAnotherVehicleSession() throws IOException {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable expired = mock(Timeoutable.class);
    Timeoutable healthy = mock(Timeoutable.class);
    UDPSessionState<Timeoutable> oldState = mock(UDPSessionState.class);
    when(oldState.getContext()).thenReturn(expired);
    when(oldState.getGetLastAccess()).thenReturn(0L);
    MavlinkDeviceKey oldKey = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    MavlinkDeviceKey activeKey = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.2", 14550), 2);
    manager.addState(oldKey, oldState);
    manager.addState(activeKey, new UDPSessionState<>(healthy));

    manager.scanForTimeouts();

    assertNull(manager.getState(oldKey));
    assertNotNull(manager.getState(activeKey));
    verify(expired).close();
    verify(healthy, never()).close();
    manager.close();
    verify(healthy).close();
  }

  @Test
  void shutdownHandlesMissingContextWithoutThrowing() {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    MavlinkDeviceKey key = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    manager.addState(key, new UDPSessionState<>(null));

    assertDoesNotThrow(manager::close);
  }

  @Test
  void shutdownContinuesAfterSessionCloseIOException() throws IOException {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable failing = mock(Timeoutable.class);
    Timeoutable healthy = mock(Timeoutable.class);
    doThrow(new IOException("simulated close failure")).when(failing).close();
    manager.addState(new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1), new UDPSessionState<>(failing));
    manager.addState(new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.2", 14550), 2), new UDPSessionState<>(healthy));

    assertDoesNotThrow(manager::close);
    verify(failing).close();
    verify(healthy).close();
  }

  @Test
  void expiryRemovesSessionWithMissingContextWithoutThrowing() {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    MavlinkDeviceKey key = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    manager.addState(key, new UDPSessionState<>(null));

    assertDoesNotThrow(manager::scanForTimeouts);
    assertNull(manager.getState(key));
    manager.close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void expiryContinuesAfterSessionCloseIOException() throws IOException {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable context = mock(Timeoutable.class);
    doThrow(new IOException("simulated expiry close failure")).when(context).close();
    UDPSessionState<Timeoutable> state = mock(UDPSessionState.class);
    when(state.getContext()).thenReturn(context);
    when(state.getGetLastAccess()).thenReturn(0L);
    MavlinkDeviceKey key = new MavlinkDeviceKey(0, new InetSocketAddress("127.0.0.1", 14550), 1);
    manager.addState(key, state);

    assertDoesNotThrow(manager::scanForTimeouts);
    verify(context).close();
    assertNull(manager.getState(key));
    manager.close();
  }
}
