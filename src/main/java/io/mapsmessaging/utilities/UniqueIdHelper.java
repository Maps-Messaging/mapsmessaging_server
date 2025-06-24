package io.mapsmessaging.utilities;

public final class UniqueIdHelper {

  private static final long PRIORITY_MASK = 0x0FL;
  private static final long BASE_ID_MASK = 0x07FFFFFFFFFFFFFFL;
  private static final int PRIORITY_SHIFT = 59;

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

