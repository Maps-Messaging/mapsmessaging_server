package org.maps.network.protocol.impl.loragateway.handler;

import static org.maps.network.protocol.impl.loragateway.Constants.CONFIG;

import java.io.IOException;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class VersionHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException {
    loRaProtocol.setSentVersion(true);
    byte[] config = loRaProtocol.getConfigBuffer();
    loRaProtocol.sendCommand(CONFIG, (byte) config.length, config);
    loRaProtocol.setSentConfig(true);
    return true;
  }
}
