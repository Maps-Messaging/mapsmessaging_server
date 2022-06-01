package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.NAK;

import io.mapsmessaging.network.io.Packet;
import lombok.ToString;

@ToString
public class NakPacket extends BasePacket{

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(NAK);
    return 1;
  }
}
