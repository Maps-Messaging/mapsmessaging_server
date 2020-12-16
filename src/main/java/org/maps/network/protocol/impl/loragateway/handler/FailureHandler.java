package org.maps.network.protocol.impl.loragateway.handler;

import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class FailureHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) {
    if (len != 0) {
      logger.log(LogMessages.LORA_GATEWAY_FAILURE, Integer.toHexString(packet.get()));
    } else {
      logger.log(LogMessages.LORA_GATEWAY_FAILURE);
    }
    return true;
  }
}
