package io.mapsmessaging.network.protocol.impl.z_wave.packet.requests;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.DataPacket;

public class IncomingRequest extends DataPacket {

  public IncomingRequest(Packet packet){
    super(packet);
  }
  @Override
  public int getType() {
    return 0;
  }

  @Override
  public int packData(Packet packet) {
    return 0;
  }
}
