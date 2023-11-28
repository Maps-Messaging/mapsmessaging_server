package io.mapsmessaging.auth.acl;

import io.mapsmessaging.security.access.AccessControlMapping;

public enum ResourceAccessControl implements AccessControlMapping {
  SUBSCRIBE,   // ordinal 0, 2^0 = 1
  PUBLISH,  // ordinal 1, 2^1 = 2
  GET_SCHEMA, // ordinal 2, 2^2 = 4
  UPDATE_SCHEMA; // ordinal 3, 2^3 = 8

  private final long value;

  ResourceAccessControl() {
    this.value = 1L << this.ordinal(); // Shift 1 left by 'ordinal' positions
  }

  @Override
  public Long getAccessValue(String accessControl) {
    if (accessControl == null) {
      return 0L;
    }
    try {
      return valueOf(accessControl.toUpperCase()).value;
    } catch (IllegalArgumentException e) {
      return 0L;
    }
  }

  @Override
  public String getAccessName(long value) {
    for (ResourceAccessControl ac : values()) {
      if (ac.value == value) {
        return ac.name().toLowerCase();
      }
    }
    return null;
  }
}
