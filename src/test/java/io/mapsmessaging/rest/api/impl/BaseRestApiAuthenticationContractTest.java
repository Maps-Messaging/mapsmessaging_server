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

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.security.access.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BaseRestApiAuthenticationContractTest {

  @Test
  void enabled_authentication_rejects_missing_session_and_missing_uuid() {
    boolean previous = BaseRestApi.AUTH_ENABLED;
    BaseRestApi.AUTH_ENABLED = true;
    try {
      HttpServletRequest request = mock(HttpServletRequest.class);
      TestApi api = new TestApi(request);
      assertEquals(401, assertThrows(WebApplicationException.class, api::access).getResponse().getStatus());
      when(request.getSession(false)).thenReturn(mock(HttpSession.class));
      assertEquals(401, assertThrows(WebApplicationException.class, api::access).getResponse().getStatus());
    } finally {
      BaseRestApi.AUTH_ENABLED = previous;
    }
  }

  @Test
  void disabled_authentication_does_not_require_an_http_session() {
    boolean previous = BaseRestApi.AUTH_ENABLED;
    BaseRestApi.AUTH_ENABLED = false;
    try {
      assertDoesNotThrow(new TestApi(null)::access);
    } finally {
      BaseRestApi.AUTH_ENABLED = previous;
    }
  }

  @Test
  void authenticated_request_preserves_existing_identity_cache() {
    boolean previous = BaseRestApi.AUTH_ENABLED;
    BaseRestApi.AUTH_ENABLED = true;
    try {
      HttpServletRequest request = mock(HttpServletRequest.class);
      HttpSession session = mock(HttpSession.class);
      Identity identity = new Identity(UUID.randomUUID(), "user", Map.of(), List.of());
      when(request.getSession(false)).thenReturn(session);
      when(request.getMethod()).thenReturn("GET");
      when(session.getAttribute("uuid")).thenReturn(UUID.randomUUID());
      when(session.getAttribute("userIdMap")).thenReturn(identity);
      doThrow(new AssertionError("Cached identity must not be overwritten"))
          .when(session).setAttribute(anyString(), any());
      assertDoesNotThrow(new TestApi(request)::access);
    } finally {
      BaseRestApi.AUTH_ENABLED = previous;
    }
  }

  @Test
  void local_grizzly_session_preserves_non_serializable_cache_objects() {
    org.glassfish.grizzly.http.server.SessionManager manager =
        org.glassfish.grizzly.servlet.ServletSessionManager.instance();
    org.glassfish.grizzly.http.server.Session session = manager.createSession(null);
    Identity identity = new Identity(UUID.randomUUID(), "user", Map.of(), List.of());
    io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree tree =
        new io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree();
    try {
      session.setAttribute("userIdMap", identity);
      session.setAttribute("DestinationTree", tree);
      org.glassfish.grizzly.http.server.Session retrieved = manager.getSession(null, session.getIdInternal());
      org.junit.jupiter.api.Assertions.assertSame(identity, retrieved.getAttribute("userIdMap"));
      org.junit.jupiter.api.Assertions.assertSame(tree, retrieved.getAttribute("DestinationTree"));
    } finally {
      session.attributes().clear();
      session.setValid(false);
    }
  }

  private static final class TestApi extends BaseRestApi {
    TestApi(HttpServletRequest request) {
      this.request = request;
    }

    void access() {
      hasAccess("destinations");
    }
  }
}
