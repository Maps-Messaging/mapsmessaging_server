package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.FUNC_ID_SERIAL_API_GET_INIT_DATA;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;

public class GetInitialData extends Command{

  @Getter
  private int version;
  @Getter
  private int capabilities;


  @Override
  public int getCommand() {
    return FUNC_ID_SERIAL_API_GET_INIT_DATA;
  }


  @Override
  public void pack(Packet packet) {
    super.pack(packet);
  }

  @Override
  public String toString(){
    return "FUNC_ID_SERIAL_API_GET_INIT_DATA";
  }

}
