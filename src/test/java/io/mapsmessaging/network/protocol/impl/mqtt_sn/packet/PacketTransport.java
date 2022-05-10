package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.Closeable;
import java.io.IOException;

public interface PacketTransport extends Closeable {

  int readPacket(Packet packet) throws IOException;

  int sendPacket(Packet packet) throws Exception;

}
