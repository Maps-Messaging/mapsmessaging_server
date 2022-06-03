package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.SOF;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.Command;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.CommandFactory;
import java.util.ArrayList;
import java.util.List;

public abstract class DataPacket extends BasePacket{

  private boolean checksum;
  private List<Command> commandList = new ArrayList<>();

  protected byte[] data;

  public DataPacket(){}

  public DataPacket(Packet packet){
    while(packet.available() > 1){ // Checksum
      Command command = CommandFactory.parseCommand(packet);
      if(command != null){
        commandList.add(command);
      }
    }
    if(packet.available() == 1) {
      checksum = (packet.get() & 0xff) != 0x0;
    }
    else{
      System.err.println("Truncated?");
    }
  }

  public void addCommand(Command command){
    commandList.add(command);
  }

  public List<Command> getCommandList(){
    return commandList;
  }

  public boolean isValid(){
    return checksum;
  }

  public abstract int getType();

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(SOF);
    packet.putByte(getType());
    for(Command command: commandList){
      command.pack(packet);
    }
    return packet.position();
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append("Commands : ").append(commandList.size()).append("\n");
    for(Command command:commandList){
      sb.append("\t").append(command).append("\n");
    }
    return sb.toString();
  }
}