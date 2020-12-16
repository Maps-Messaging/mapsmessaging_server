package org.maps.network.protocol.impl.loragateway.handler;

import java.io.IOException;
import org.maps.logging.Logger;
import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.loragateway.LoRaProtocol;

public interface PacketHandler {

  boolean processPacket(LoRaProtocol loRaProtocol, Packet packet, int len, Logger logger) throws IOException;

}
