package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ForceSucNodeId extends Command {

  @Getter
  @Setter
  private boolean success;

  @Override
  public int getCommand() {
    return Constants.FUNC_ID_ZW_SET_SUC_NODE_ID;
  }

  @Override
  public void unpack(Packet packet){
  }

  public void pack(Packet packet){
    super.pack(packet);
    packet.putByte(1);
    packet.putByte(1);
    success = false;
  }

}
