package io.mapsmessaging.auth.priviliges;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringPrivilegeCoverageSweepTest {
  @Test
  void equalityIncludesPrivilegeNameAndStringValue() {
    StringPrivilege value = new StringPrivilege("realm", "operations");

    assertEquals("realm", value.getName());
    assertEquals("operations", value.getValue());
    assertEquals(value, new StringPrivilege("realm", "operations"));
    assertNotEquals(value, new StringPrivilege("realm", "other"));
  }
}
