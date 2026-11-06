package io.mapsmessaging.auth.registry.principal;

import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionPrivilegePrincipalCoverageSweepTest {
  @Test
  void exposesStableNameAndIncludesPrivilegesInStringForm() {
    SessionPrivileges privileges = SessionPrivileges.create("user");
    SessionPrivilegePrincipal principal = new SessionPrivilegePrincipal(privileges);

    assertEquals("SessionPrivilegePrincipal", principal.getName());
    assertTrue(principal.toString().contains("Session Privileges"));
    assertTrue(principal.toString().contains("user"));
  }
}
