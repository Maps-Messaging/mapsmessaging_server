package io.mapsmessaging.auth;

import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.priviliges.subscription.SubscriptionPrivileges;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class QuotaPrincipalCoverageSweepTest {
  @Test
  void exposesQuotaSourcesAndStablePrincipalName() {
    SessionPrivileges session = SessionPrivileges.create("user");
    SubscriptionPrivileges subscription = mock(SubscriptionPrivileges.class);

    QuotaPrincipal principal = new QuotaPrincipal(session, subscription);

    assertEquals("QuotaPrincipal", principal.getName());
    assertSame(session, principal.getSessionPrivileges());
    assertSame(subscription, principal.getSubscriptionPrivileges());
    assertEquals(principal, new QuotaPrincipal(session, subscription));
  }
}
