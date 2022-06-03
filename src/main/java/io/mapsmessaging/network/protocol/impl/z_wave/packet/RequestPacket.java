package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.REQUEST;

import io.mapsmessaging.network.io.Packet;

public class RequestPacket extends DataPacket{

  public RequestPacket(){
  }

  public RequestPacket(Packet packet) {
    super(packet);
  }

  @Override
  public int getType() {
    return REQUEST;
  }


  @Override
  public String toString(){
    return super.toString();
  }
}
