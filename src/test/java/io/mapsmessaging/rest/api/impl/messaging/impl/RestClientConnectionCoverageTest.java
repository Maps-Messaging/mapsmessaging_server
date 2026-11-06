/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.rest.api.impl.messaging.impl;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestClientConnectionCoverageTest {

  @Test
  void identityUsesSessionIdAndUsernameWhenPresent() {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("session-123");
    when(session.getAttribute("username")).thenReturn("alice");

    RestClientConnection connection = new RestClientConnection(session);

    assertEquals("session-123", connection.getName());
    assertEquals("RestClientConnection_session-123", connection.getUniqueName());
    assertEquals("alice", connection.getPrincipal().getName());
  }

  @Test
  void identityFallsBackToSessionIdWhenUsernameMissing() {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("session-456");
    when(session.getAttribute("username")).thenReturn(null);

    RestClientConnection connection = new RestClientConnection(session);

    assertEquals("session-456", connection.getPrincipal().getName());
  }

  @Test
  void connectionContractUsesRestDefaults() {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("s");

    RestClientConnection connection = new RestClientConnection(session);

    assertEquals(0L, connection.getTimeOut());
    assertEquals("1.0", connection.getVersion());
    assertEquals("", connection.getAuthenticationConfig());
    assertEquals("RestAPI", connection.getProtocolName());
    assertEquals("", connection.getRemoteIp());
    assertDoesNotThrow(connection::sendKeepAlive);
  }
}
