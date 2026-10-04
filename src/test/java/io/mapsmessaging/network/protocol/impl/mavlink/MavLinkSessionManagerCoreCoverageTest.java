/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MavLinkSessionManagerCoreCoverageTest {

  @Test
  void addAndGetRefreshSessionAccessTime() throws Exception {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable context = mock(Timeoutable.class);
    UDPSessionState<Timeoutable> state = new UDPSessionState<>(context);
    MavlinkDeviceKey key = key(1);

    try {
      setLastAccess(state, 1L);
      manager.addState(key, state);
      long afterAdd = state.getGetLastAccess();

      assertTrue(afterAdd > 1L);

      setLastAccess(state, 2L);
      assertSame(state, manager.getState(key));
      assertTrue(state.getGetLastAccess() > 2L);
    } finally {
      manager.close();
    }
  }

  @Test
  void deleteStateMakesSessionUnavailable() {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    MavlinkDeviceKey key = key(2);
    manager.addState(key, new UDPSessionState<>(mock(Timeoutable.class)));

    try {
      manager.deleteState(key);
      assertNull(manager.getState(key));
    } finally {
      manager.close();
    }
  }

  @Test
  void scanExpiresSessionUsingManagerTimeoutAndClosesContext() throws Exception {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(1);
    Timeoutable context = mock(Timeoutable.class);
    UDPSessionState<Timeoutable> state = new UDPSessionState<>(context);
    MavlinkDeviceKey key = key(3);
    manager.addState(key, state);
    setLastAccess(state, System.currentTimeMillis() - 2_000L);

    try {
      manager.scanForTimeouts();

      assertNull(manager.getState(key));
      verify(context).close();
    } finally {
      manager.close();
    }
  }

  @Test
  void contextSpecificTimeoutOverridesManagerTimeout() throws Exception {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable context = mock(Timeoutable.class);
    when(context.getTimeOut()).thenReturn(10L);
    UDPSessionState<Timeoutable> state = new UDPSessionState<>(context);
    MavlinkDeviceKey key = key(4);
    manager.addState(key, state);
    setLastAccess(state, System.currentTimeMillis() - 100L);

    try {
      manager.scanForTimeouts();

      assertNull(manager.getState(key));
      verify(context).close();
    } finally {
      manager.close();
    }
  }

  @Test
  void activeSessionSurvivesTimeoutScan() {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable context = mock(Timeoutable.class);
    UDPSessionState<Timeoutable> state = new UDPSessionState<>(context);
    MavlinkDeviceKey key = key(5);
    manager.addState(key, state);

    try {
      manager.scanForTimeouts();

      assertSame(state, manager.getState(key));
      verify(context, never()).close();
    } finally {
      manager.close();
    }
  }

  @Test
  void closeAttemptsEveryContextAndClearsSessionsDespiteCloseFailure() throws Exception {
    MavLinkSessionManager<Timeoutable> manager = new MavLinkSessionManager<>(60);
    Timeoutable first = mock(Timeoutable.class);
    Timeoutable second = mock(Timeoutable.class);
    doThrow(new IOException("close failed")).when(first).close();
    MavlinkDeviceKey firstKey = key(6);
    MavlinkDeviceKey secondKey = key(7);
    manager.addState(firstKey, new UDPSessionState<>(first));
    manager.addState(secondKey, new UDPSessionState<>(second));

    assertDoesNotThrow(manager::close);

    verify(first).close();
    verify(second).close();
    assertNull(manager.getState(firstKey));
    assertNull(manager.getState(secondKey));
  }

  private static MavlinkDeviceKey key(int systemId) {
    return new MavlinkDeviceKey(
        14550,
        new InetSocketAddress("127.0.0.1", 15000 + systemId),
        systemId);
  }

  private static void setLastAccess(UDPSessionState<?> state, long value) throws Exception {
    Field field = UDPSessionState.class.getDeclaredField("getLastAccess");
    field.setAccessible(true);
    field.setLong(state, value);
  }
}
