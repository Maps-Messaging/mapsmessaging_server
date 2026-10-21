package io.mapsmessaging.auth.registry.principal;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccessIdPrincipalCoverageSweepTest {
  @Test
  void exposesConfiguredAccessIdsAndLegacyPrincipalName() {
    List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
    AccessIdPrincipal principal = new AccessIdPrincipal(ids);

    assertSame(ids, principal.getAccessIds());
    assertEquals("AccessIdPrinicpal", principal.getName());
  }
}
