package org.maps.network.io;

import java.util.concurrent.atomic.AtomicLong;

public class Constants {
  private static final AtomicLong idGenerator = new AtomicLong(0);

  public static long getNextId() {
    return idGenerator.incrementAndGet();
  }
}
