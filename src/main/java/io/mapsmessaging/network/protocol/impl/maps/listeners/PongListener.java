/* Copyright [ 2020 - 2026 ] MapsMessaging B.V. */
package io.mapsmessaging.network.protocol.impl.maps.listeners;

import io.mapsmessaging.network.protocol.impl.maps.MapsFrame;
import io.mapsmessaging.network.protocol.impl.maps.MapsProtocol;

public class PongListener implements MapsPacketListener {
  @Override
  public void handle(MapsFrame frame, MapsProtocol protocol) {
    protocol.handlePong(frame);
  }
}
