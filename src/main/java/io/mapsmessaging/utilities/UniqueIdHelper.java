package io.mapsmessaging.utilities;

import io.mapsmessaging.api.features.Priority;

public final class UniqueIdHelper {

  private static final int PRIORITY_BITS = 32 - Integer.numberOfLeadingZeros(Priority.HIGHEST.ordinal()); // e.g., 4 for value 10
  private static final int PRIORITY_SHIFT = Long.SIZE - PRIORITY_BITS; // e.g., 64 - 4 = 60
  private static final long PRIORITY_MASK = (1L << PRIORITY_BITS) - 1; // e.g., 0b1111
  private static final long BASE_ID_MASK = (1L << PRIORITY_SHIFT) - 1; // lower bits only

  private UniqueIdHelper() {
    // utility class
  }

  public static long compute(long baseId, int priority) {
    if ((priority & ~0x0F) != 0) {
      throw new IllegalArgumentException("Priority must be between 0 and 15");
    }
    return ((long) priority << PRIORITY_SHIFT) | (baseId & BASE_ID_MASK);
  }

  public static int priority(long uniqueId) {
    return (int) ((uniqueId >>> PRIORITY_SHIFT) & PRIORITY_MASK);
  }

  public static long baseId(long uniqueId) {
    return uniqueId & BASE_ID_MASK;
  }

}

