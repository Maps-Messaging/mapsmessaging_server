package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;

public class SensorUpdate extends Command {


  @Getter
  @Setter
  private boolean success;

  @Override
  public int getCommand() {
    return Constants.FUNC_ID_APPLICATION_COMMAND_HANDLER;
  }

  @Override
  public void unpack(Packet packet){
    success = packet.get() != 0x1;
  }

  public void pack(Packet packet){
    super.pack(packet);
    packet.putByte(01);
    packet.putByte(01);
    success = false;
  }

  @Override
  public String toString(){
    return "FUNC_ID_APPLICATION_COMMAND_HANDLER";
  }
}
