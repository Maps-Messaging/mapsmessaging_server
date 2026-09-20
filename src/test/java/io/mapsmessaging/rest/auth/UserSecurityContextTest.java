package io.mapsmessaging.rest.auth;

import io.mapsmessaging.test.BaseTestConfig;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSecurityContextTest extends BaseTestConfig {

  @Test
  void authenticatedUserExposesPrincipalSubjectUuidAndBasicScheme() {
    UserSecurityContext context = new UserSecurityContext("admin");

    assertEquals("admin", context.getUserPrincipal().getName());
    assertNotNull(context.getSubject());
    assertNotNull(context.getUuid());
    assertEquals(SecurityContext.BASIC_AUTH, context.getAuthenticationScheme());
    assertFalse(context.isSecure());
    assertFalse(context.isUserInRole("admin"));
  }
}
