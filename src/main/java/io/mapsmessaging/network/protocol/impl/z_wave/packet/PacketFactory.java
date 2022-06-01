package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.ACK;
import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.CAN;
import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.NAK;
import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.SOF;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.requests.IncomingRequest;

public class PacketFactory {


  public BasePacket parse(Packet packet){
    byte frame = packet.get();
    switch(frame){
      case SOF:
        return new IncomingRequest(packet);

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
