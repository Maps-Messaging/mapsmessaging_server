/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.shm;

import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.memory.MemoryEndPoint;

public final class SharedMemoryEndPoint extends MemoryEndPoint {

  public SharedMemoryEndPoint(long id, EndPointServerStatus server, SharedMemoryTransport transport) {
    super(id, server, transport);
  }

  @Override
  public String getProtocol() {
    return "shm";
  }
}
