package io.mapsmessaging.auth.registry;

import io.mapsmessaging.security.access.Identity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UserDetailsCoverageSweepTest {
  @Test
  void retainsIdentityAndGroupMembership() {
    Identity identity = mock(Identity.class);
    UUID group = UUID.randomUUID();
    UserDetails details = new UserDetails(identity, List.of(group));

    assertSame(identity, details.getIdentityEntry());
    assertEquals(List.of(group), details.getGroups());
  }
}
