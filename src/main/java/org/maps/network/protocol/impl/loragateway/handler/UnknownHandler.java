package org.maps.network.protocol.impl.loragateway.handler;

import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class UnknownHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) {
    logger.log(LogMessages.LORA_GATEWAY_UNEXPECTED);
    return true;
  }
}
