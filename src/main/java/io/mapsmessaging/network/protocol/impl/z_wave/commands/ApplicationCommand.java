package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.FUNC_ID_APPLICATION_COMMAND_HANDLER;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ApplicationCommand extends Command {

  @Getter
  @Setter
  private int status;

  @Getter
  @Setter
  private int nodeId;

  @Getter
  @Setter
  private byte[] data;

  public ApplicationCommand(){}

  @Override
  public void unpack(Packet packet) {
    status = packet.getByte();
    nodeId = packet.getByte();
    data =new byte[packet.getByte()];
    packet.get(data);
    String sval = "";
    for(byte b:data){
      int val = b & 0xff;
      String t = Integer.toHexString(val);
      if(t.length() == 1){
        t = "0"+t;
      }
      sval += t + " ";
    }
    System.err.println(sval);
  }

  public int getCommand(){
    return FUNC_ID_APPLICATION_COMMAND_HANDLER;
  }
}
