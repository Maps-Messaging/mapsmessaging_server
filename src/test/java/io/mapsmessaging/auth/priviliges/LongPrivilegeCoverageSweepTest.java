package io.mapsmessaging.auth.priviliges;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LongPrivilegeCoverageSweepTest {
  @Test
  void equalityIncludesPrivilegeNameAndLongValue() {
    LongPrivilege value = new LongPrivilege("limit", 123L);

    assertEquals("limit", value.getName());
    assertEquals(123L, value.getValue());
    assertEquals(value, new LongPrivilege("limit", 123L));
    assertNotEquals(value, new LongPrivilege("limit", 124L));
  }
}
