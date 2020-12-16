package org.maps.network.protocol.impl.loragateway.handler;

import java.io.IOException;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public class DataHandler implements PacketHandler {

  @Override
  public boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException {
    int loraClientId = packet.get();
    int rssi = packet.get();
    packet.getRawBuffer().compact();
    packet.position(0);
    packet.limit(len - 2);
    packet.getRawBuffer().limit(len - 2);
    packet.setFromAddress(loRaProtocol.getSocketAddress(loraClientId));
    loRaProtocol.handleIncomingPacket(packet, loraClientId, rssi);
    packet.clear();
    return true;
  }
}
