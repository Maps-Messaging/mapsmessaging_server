/* Copyright [ 2020 - 2026 ] MapsMessaging B.V. */
package io.mapsmessaging.network.protocol.impl.maps.listeners;

import io.mapsmessaging.network.protocol.impl.maps.MapsFrame;
import io.mapsmessaging.network.protocol.impl.maps.MapsProtocol;
import java.io.IOException;

public class ConnectListener implements MapsPacketListener {
  @Override
  public void handle(MapsFrame frame, MapsProtocol protocol) throws IOException {
    protocol.handleConnect(frame);
  }
}
