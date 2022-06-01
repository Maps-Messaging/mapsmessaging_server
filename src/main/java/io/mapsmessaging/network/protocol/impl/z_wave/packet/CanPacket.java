package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.CAN;

import io.mapsmessaging.network.io.Packet;
import lombok.ToString;

@ToString
public class CanPacket extends BasePacket{

  @Override
  public int packFrame(Packet packet) {
      packet.putByte(CAN);
      return 1;
  }
}
