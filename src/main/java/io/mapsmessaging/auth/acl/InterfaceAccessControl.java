package io.mapsmessaging.auth.acl;

import io.mapsmessaging.security.access.AccessControlMapping;

public enum InterfaceAccessControl implements AccessControlMapping {
  CONNECT,
  MANAGE;

  private final long value;

  InterfaceAccessControl() {
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
    for (InterfaceAccessControl ac : values()) {
      if (ac.value == value) {
        return ac.name().toLowerCase();
      }
    }
    return null;
  }
}