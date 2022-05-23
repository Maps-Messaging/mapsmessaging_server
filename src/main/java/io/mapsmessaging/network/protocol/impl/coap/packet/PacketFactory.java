package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public class PacketFactory {



  public BasePacket parseFrame(Packet packet) throws EndOfBufferException, MalformedException {
    byte[] header = new byte[4];
    packet.get(header);


    return null;
  }
}
