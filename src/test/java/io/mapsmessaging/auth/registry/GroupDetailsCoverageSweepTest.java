package io.mapsmessaging.auth.registry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupDetailsCoverageSweepTest {
  @Test
  void retainsGroupIdentityAndMemberList() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    GroupDetails details = new GroupDetails("operators", groupId, List.of(userId));

    assertEquals("operators", details.getName());
    assertEquals(groupId, details.getGroupId());
    assertEquals(List.of(userId), details.getUsers());
    assertTrue(details.toString().contains("operators"));
  }
}
