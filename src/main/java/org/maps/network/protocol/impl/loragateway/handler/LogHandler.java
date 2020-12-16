package org.maps.network.protocol.impl.loragateway.handler;

import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class LogHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) {
    byte[] tmp = new byte[packet.available()];
    packet.get(tmp);
    logger.log(LogMessages.LORA_GATEWAY_LOG, new String(tmp));
    return true;
  }
}
