/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Disconnect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ReasonString;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SessionExpiryInterval;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.TopicAlias;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DisconnectListener5CoverageTest {

  @Test
  void disconnectWithoutSessionClosesProtocolAndEndpoint() throws Exception {
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);

    assertNull(new DisconnectListener5().handlePacket(
        new Disconnect5(StatusCode.SUCCESS), null, endPoint, protocol));

    verify(protocol).close();
    verify(endPoint).close();
  }

  @Test
  void reasonStringWithoutSessionStillClosesConnection() throws Exception {
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    Disconnect5 disconnect = new Disconnect5(StatusCode.SUCCESS);
    disconnect.add(new ReasonString("normal shutdown"));

    assertNull(new DisconnectListener5().handlePacket(disconnect, null, endPoint, protocol));

    verify(protocol).close();
    verify(endPoint).close();
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, 1L, 60L, 86400L, 4294967295L})
  void sessionExpiryPropertyIsApplied(long expiry) throws Exception {
    Session session = mock(Session.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    SessionManager manager = mock(SessionManager.class);
    Disconnect5 disconnect = new Disconnect5(StatusCode.SUCCESS);
    disconnect.add(new SessionExpiryInterval(expiry));

    try (MockedStatic<SessionManager> sessionManagers = mockStatic(SessionManager.class)) {
      sessionManagers.when(SessionManager::getInstance).thenReturn(manager);

      assertNull(new DisconnectListener5().handlePacket(disconnect, session, endPoint, protocol));

      verify(session).setExpiryTime(expiry);
      verify(manager).close(session, true);
    }
  }

  @Test
  void successfulDisconnectClearsWillTask() throws Exception {
    Session session = mock(Session.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    SessionManager manager = mock(SessionManager.class);

    try (MockedStatic<SessionManager> sessionManagers = mockStatic(SessionManager.class)) {
      sessionManagers.when(SessionManager::getInstance).thenReturn(manager);

      new DisconnectListener5().handlePacket(
          new Disconnect5(StatusCode.SUCCESS), session, endPoint, protocol);

      verify(manager).close(session, true);
    }
  }

  @Test
  void unsuccessfulDisconnectPreservesWillTask() throws Exception {
    Session session = mock(Session.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    SessionManager manager = mock(SessionManager.class);

    try (MockedStatic<SessionManager> sessionManagers = mockStatic(SessionManager.class)) {
      sessionManagers.when(SessionManager::getInstance).thenReturn(manager);

      new DisconnectListener5().handlePacket(
          new Disconnect5(StatusCode.UNSPECIFIED_ERROR), session, endPoint, protocol);

      verify(manager).close(session, false);
    }
  }

  @Test
  void unrecognisedDisconnectPropertyIsIgnored() throws Exception {
    Session session = mock(Session.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    SessionManager manager = mock(SessionManager.class);
    Disconnect5 disconnect = new Disconnect5(StatusCode.SUCCESS);
    disconnect.add(new TopicAlias(7));

    try (MockedStatic<SessionManager> sessionManagers = mockStatic(SessionManager.class)) {
      sessionManagers.when(SessionManager::getInstance).thenReturn(manager);

      assertNull(new DisconnectListener5().handlePacket(disconnect, session, endPoint, protocol));

      verify(manager).close(session, true);
      verify(protocol).close();
      verify(endPoint).close();
    }
  }

  @Test
  void sessionCloseFailureDoesNotPreventTransportClose() throws Exception {
    Session session = mock(Session.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    SessionManager manager = mock(SessionManager.class);
    doThrow(new IOException("session close failed")).when(manager).close(session, true);

    try (MockedStatic<SessionManager> sessionManagers = mockStatic(SessionManager.class)) {
      sessionManagers.when(SessionManager::getInstance).thenReturn(manager);

      new DisconnectListener5().handlePacket(
          new Disconnect5(StatusCode.SUCCESS), session, endPoint, protocol);

      verify(protocol).close();
      verify(endPoint).close();
    }
  }

  @Test
  void protocolCloseFailureIsContained() throws Exception {
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    doThrow(new IOException("protocol close failed")).when(protocol).close();

    assertNull(new DisconnectListener5().handlePacket(
        new Disconnect5(StatusCode.SUCCESS), null, endPoint, protocol));

    verify(protocol).close();
    verify(endPoint, never()).close();
  }

  @Test
  void endpointCloseFailureIsContained() throws Exception {
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    doThrow(new IOException("endpoint close failed")).when(endPoint).close();

    assertNull(new DisconnectListener5().handlePacket(
        new Disconnect5(StatusCode.SUCCESS), null, endPoint, protocol));

    verify(protocol).close();
    verify(endPoint).close();
  }
}
