package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.RESPONSE;

import io.mapsmessaging.network.io.Packet;

public abstract class ResponsePacket extends DataPacket {

  public ResponsePacket(Packet packet) {
    super(packet);
  }

  @Override
  public int getType() {
    return RESPONSE;
  }

}
