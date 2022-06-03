package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.ACK;

import io.mapsmessaging.network.io.Packet;
import lombok.ToString;

@ToString
public class AckPacket extends BasePacket{

  public AckPacket(){
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(ACK);
    return 1;
  }
}
