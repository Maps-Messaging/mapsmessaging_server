/*
 *
 * Copyright [ 2020 - 2024 ] Matthew Buckton
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.rest.responses.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccessRequestApiCoverageTest {

  @Test
  void userSessionReportsAuthNotEnforcedWhenHttpSessionMissing() {
    AccessRequestApi api = new AccessRequestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(null);
    api.request = request;

    LoginResponse response = api.getUserSession();

    assertEquals("Auth not enforced", response.getStatus());
  }

  @Test
  void userSessionReportsAuthNotEnforcedWhenSubjectMissing() {
    AccessRequestApi api = new AccessRequestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("subject")).thenReturn(null);
    api.request = request;

    LoginResponse response = api.getUserSession();

    assertEquals("Auth not enforced", response.getStatus());
    verify(session, never()).getAttribute("username");
  }
}
