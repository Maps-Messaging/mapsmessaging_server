package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.ACK;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.CAN;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.NAK;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.REQUEST;
import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.SOF;

import io.mapsmessaging.network.io.Packet;

public class PacketFactory {


  public BasePacket parse(Packet packet){
    byte frame = packet.get();
    switch(frame){
      case SOF:
        int len = packet.get() & 0xff;
        int type = packet.get() & 0xff;
        if(type == REQUEST){
          return new RequestPacket(packet);
        }
        return new ResponsePacket(packet);
      case ACK:
        return new AckPacket();
      case NAK:
        return new NakPacket();
      case CAN:
        return new CanPacket();
      default:
        return null;
    }
  }
}
