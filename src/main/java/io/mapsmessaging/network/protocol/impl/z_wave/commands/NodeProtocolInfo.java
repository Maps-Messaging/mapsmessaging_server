package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;


public class NodeProtocolInfo extends Command {

  @Getter
  @Setter
  private int nodeId;

  public NodeProtocolInfo(int nodeId){
    this.nodeId = nodeId;
  }

  @Override
  public int getCommand() {
    return Constants.FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO;
  }

  @Override
  public void unpack(Packet packet){
    super.unpack(packet);
  }

  public void pack(Packet packet){
    super.pack(packet);
    packet.putByte(nodeId);
  }

  @Override
  public String toString(){
    return "FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO";
  }
}
