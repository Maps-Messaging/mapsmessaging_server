package io.mapsmessaging.auth.priviliges;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BooleanPrivilegeCoverageSweepTest {
  @Test
  void equalityIncludesPrivilegeNameAndBooleanValue() {
    BooleanPrivilege value = new BooleanPrivilege("enabled", true);

    assertEquals("enabled", value.getName());
    assertTrue(value.isValue());
    assertEquals(value, new BooleanPrivilege("enabled", true));
    assertNotEquals(value, new BooleanPrivilege("enabled", false));
  }
}
