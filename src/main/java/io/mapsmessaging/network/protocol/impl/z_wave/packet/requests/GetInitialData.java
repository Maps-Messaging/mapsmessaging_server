package io.mapsmessaging.network.protocol.impl.z_wave.packet.requests;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.FUNC_ID_SERIAL_API_GET_INIT_DATA;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.RequestPacket;

public class GetInitialData extends RequestPacket {

  public GetInitialData(){}


  @Override
  public int packData(Packet packet) {
    packet.putByte(FUNC_ID_SERIAL_API_GET_INIT_DATA);
    return 1;
  }

}
