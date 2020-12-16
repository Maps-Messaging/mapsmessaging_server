package org.maps.network.protocol.impl.loragateway.handler;

import static org.maps.network.protocol.impl.loragateway.Constants.PING;
import static org.maps.network.protocol.impl.loragateway.Constants.START;
import static org.maps.network.protocol.impl.loragateway.Constants.VERSION;

import java.io.IOException;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class PingHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException {
    int state = packet.get();
    logger.log(LogMessages.LORA_GATEWAY_PING, state);
    if (!loRaProtocol.isSentVersion()) {
      loRaProtocol.sendCommand(VERSION); // Request version of gateway
    } else if (loRaProtocol.isSentConfig() && !loRaProtocol.isStarted()) {
      loRaProtocol.sendCommand(START, (byte) 0, null);
      loRaProtocol.setStarted(true);
    } else {
      loRaProtocol.sendCommand(PING);
    }
    return true;
  }
}
