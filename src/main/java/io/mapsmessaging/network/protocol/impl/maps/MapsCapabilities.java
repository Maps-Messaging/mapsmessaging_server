/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

public final class MapsCapabilities {

  public static final long SELECTORS = 1L;
  public static final long NATIVE_MESSAGE = 1L << 1;
  public static final long QOS_ACK = 1L << 2;
  public static final long RETAINED = 1L << 3;
  public static final long COMPRESSION = 1L << 4;

  public static final long INTEREST_PROPAGATION = 1L << 16;
  public static final long ROUTING = 1L << 17;
  public static final long NODE_DISCOVERY = 1L << 18;

  public static final long CLUSTERING = 1L << 32;
  public static final long STATE_TRANSFER = 1L << 33;
  public static final long SESSION_REPLICATION = 1L << 34;

  public static final long VERSION_1 = SELECTORS | NATIVE_MESSAGE | QOS_ACK | RETAINED | COMPRESSION;

  private MapsCapabilities() {
  }
}
