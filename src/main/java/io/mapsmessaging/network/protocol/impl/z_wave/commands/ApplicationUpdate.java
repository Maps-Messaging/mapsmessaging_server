package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.FUNC_ID_ZW_APPLICATION_UPDATE;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@ToString
public class ApplicationUpdate extends Command {

  @Getter
  @Setter
  private int nodeId;

  @Getter
  @Setter
  private int state;

  @Getter
  @Setter
  private byte[] data;

  public ApplicationUpdate(){}

  @Override
  public void unpack(Packet packet) {
    nodeId = packet.getByte();
    state = packet.getByte();
    data =new byte[packet.getByte()];
    packet.get(data);
  }

  public int getCommand(){
    return FUNC_ID_ZW_APPLICATION_UPDATE;
  }
}
