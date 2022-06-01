package io.mapsmessaging.network.protocol.impl.z_wave.packet.requests;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.FUNC_ID_SERIAL_API_SOFT_RESET;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.RequestPacket;
import lombok.ToString;

@ToString
public class SoftResetPacket extends RequestPacket {

  public SoftResetPacket(){

  }
  public SoftResetPacket(Packet packet) {
    super(packet);
  }

  @Override
  public int packData(Packet packet) {
    packet.putByte(FUNC_ID_SERIAL_API_SOFT_RESET);
    return 1;
  }

}
