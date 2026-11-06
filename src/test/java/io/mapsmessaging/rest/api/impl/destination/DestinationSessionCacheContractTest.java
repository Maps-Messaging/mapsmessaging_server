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

package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.api.DestinationInfo;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree;
import io.mapsmessaging.rest.cache.CacheKey;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.login.LoginException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DestinationSessionCacheContractTest {

  @Test
  void destination_snapshot_is_reused_with_stable_etag_and_isolated_per_http_session() throws Exception {
    Session messaging = mock(Session.class);
    when(messaging.getAllDestinationNames()).thenReturn(List.of(
        new DestinationInfo("sensors/temperature", DestinationType.TOPIC)));
    Map<String, Object> firstAttributes = new HashMap<>();
    TestApi first = new TestApi(httpSession(firstAttributes), messaging);
    DestinationPageRequestDTO request = new DestinationPageRequestDTO();
    request.setPrefix("sensors");

    String etag;
    try (Response response = first.getDestinationPage(request, null)) {
      assertEquals(200, response.getStatus());
      DestinationPageResponseDTO page = assertInstanceOf(DestinationPageResponseDTO.class, response.getEntity());
      assertEquals("temperature", page.getEntries()[0].getName());
      etag = response.getHeaderString("ETag");
    }
    NamespaceTree firstTree = assertInstanceOf(NamespaceTree.class, firstAttributes.get("DestinationTree"));
    try (Response response = first.getDestinationPage(request, etag)) {
      assertEquals(304, response.getStatus());
      assertEquals(etag, response.getHeaderString("ETag"));
    }
    assertEquals(1, first.lookups);
    assertSame(firstTree, firstAttributes.get("DestinationTree"));

    Map<String, Object> secondAttributes = new HashMap<>();
    TestApi second = new TestApi(httpSession(secondAttributes), messaging);
    try (Response response = second.getDestinationPage(request, null)) {
      assertEquals(200, response.getStatus());
    }
    assertEquals(1, second.lookups);
    assertNotSame(firstTree, secondAttributes.get("DestinationTree"));
  }

  private static HttpSession httpSession(Map<String, Object> attributes) {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
    doAnswer(call -> {
      attributes.put(call.getArgument(0), call.getArgument(1));
      return null;
    }).when(session).setAttribute(anyString(), any());
    return session;
  }

  private static final class TestApi extends DestinationListManagementAPI {
    private final HttpSession httpSession;
    private final Session messaging;
    private int lookups;

    TestApi(HttpSession httpSession, Session messaging) {
      this.httpSession = httpSession;
      this.messaging = messaging;
      uriInfo = mock(UriInfo.class);
      when(uriInfo.getPath()).thenReturn("server/destination/list");
    }

    @Override
    protected void hasAccess(String resource) {}

    @Override
    protected HttpSession getSession() {
      return httpSession;
    }

    @Override
    protected Session getAuthenticatedSession() throws LoginException, IOException {
      lookups++;
      return messaging;
    }

    @Override
    protected <T> T getFromCache(CacheKey key, Class<T> type) {
      return null;
    }

    @Override
    protected void putToCache(CacheKey key, Object value) {}
  }
}
