/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.shm;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class SharedMemoryEndPointConnectionFactory implements EndPointConnectionFactory {

  private static final int DEFAULT_SLOT_SIZE = 64 * 1024;
  private static final int DEFAULT_SLOT_COUNT = 256;

  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback callback, EndPointServerStatus endPointServerStatus,
      List<String> jmxPath) throws IOException {
    Map<String, String> parameters = url.getParameters();
    String side = parameters.getOrDefault("side", "a");
    if (!(side.equalsIgnoreCase("a") || side.equalsIgnoreCase("b"))) {
      throw new IOException("Shared memory endpoint side must be 'a' or 'b'");
    }

    int slotSize = parsePositive(parameters, "slotsize", DEFAULT_SLOT_SIZE);
    int slotCount = parsePositive(parameters, "slots", DEFAULT_SLOT_COUNT);
    SharedMemoryTransport transport = new SharedMemoryTransport(url.getHost(), side.equalsIgnoreCase("a"), slotSize, slotCount);
    EndPoint endPoint = new SharedMemoryEndPoint(generateID(), endPointServerStatus, transport);
    callback.connected(endPoint);
    return endPoint;
  }

  @Override
  public String getName() {
    return "shm";
  }

  @Override
  public String getDescription() {
    return "Shared memory inter-server connection endpoint factory";
  }

  private static int parsePositive(Map<String, String> parameters, String name, int defaultValue) throws IOException {
    String value = parameters.get(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(value);
      if (parsed <= 0) {
        throw new NumberFormatException("not positive");
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new IOException("Invalid " + name + " value: " + value, e);
    }
  }
}
