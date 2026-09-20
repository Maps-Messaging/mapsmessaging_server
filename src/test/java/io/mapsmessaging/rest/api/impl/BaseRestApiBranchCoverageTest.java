package io.mapsmessaging.rest.api.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseRestApiBranchCoverageTest {

  @Test
  void clientIpPrefersForwardedThenRealThenRemoteAddress() {
    TestApi api = new TestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    api.setRequest(request);

    when(request.getHeader("X-Forwarded-For")).thenReturn(" 203.0.113.4, 10.0.0.1 ");
    assertEquals("203.0.113.4", api.extractClientIp());

    when(request.getHeader("X-Forwarded-For")).thenReturn(" ");
    when(request.getHeader("X-Real-IP")).thenReturn(" 198.51.100.7 ");
    assertEquals("198.51.100.7", api.extractClientIp());

    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("192.0.2.9");
    assertEquals("192.0.2.9", api.extractClientIp());
  }

  @Test
  void uuidParserAcceptsValidValuesAndRejectsNullBlankAndMalformedValues() {
    TestApi api = new TestApi();
    UUID uuid = UUID.randomUUID();

    assertEquals(uuid, api.parse(uuid.toString()));
    assertNull(api.parse(null));
    assertNull(api.parse(" "));
    assertNull(api.parse("not-a-uuid"));
  }

  @Test
  void computeAccessMapsHttpMethodsToStablePermissionIndexes() throws Exception {
    Method method = BaseRestApi.class.getDeclaredMethod("computeAccess", String.class);
    method.setAccessible(true);
    BaseRestApi api = new BaseRestApi();

    assertEquals(0L, method.invoke(api, "GET"));
    assertEquals(0L, method.invoke(api, "HEAD"));
    assertEquals(1L, method.invoke(api, "POST"));
    assertEquals(2L, method.invoke(api, "PUT"));
    assertEquals(3L, method.invoke(api, "DELETE"));
    assertEquals(0L, method.invoke(api, "PATCH"));
  }

  @Test
  void missingHttpSessionIsUnauthorized() {
    TestApi api = new TestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    api.setRequest(request);
    when(request.getSession(false)).thenReturn(null);

    WebApplicationException failure =
        assertThrows(WebApplicationException.class, api::session);

    assertEquals(401, failure.getResponse().getStatus());
  }

  private static final class TestApi extends BaseRestApi {
    void setRequest(HttpServletRequest request) {
      this.request = request;
    }

    UUID parse(String value) {
      return parseUuidOrNull(value);
    }

    jakarta.servlet.http.HttpSession session() {
      return getSession();
    }
  }
}