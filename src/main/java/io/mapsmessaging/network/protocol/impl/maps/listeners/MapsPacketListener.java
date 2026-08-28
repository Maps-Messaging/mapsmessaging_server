/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps.listeners;

import io.mapsmessaging.network.protocol.impl.maps.MapsFrame;
import io.mapsmessaging.network.protocol.impl.maps.MapsProtocol;
import java.io.IOException;

@FunctionalInterface
public interface MapsPacketListener {
  void handle(MapsFrame frame, MapsProtocol protocol) throws IOException;
}
