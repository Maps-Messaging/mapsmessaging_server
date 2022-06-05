package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.FUNC_ID_SERIAL_API_GET_INIT_DATA;

import io.mapsmessaging.network.io.Packet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@ToString
public class GetInitialData extends Command{

  @Getter
  private int version;
  @Getter
  private int capabilities;
  @Getter
  List<Integer> nodeList;
  @Getter
  private int chipType;

  public GetInitialData(){
    nodeList=new ArrayList<>();
  }

  @Override
  public int getCommand() {
    return FUNC_ID_SERIAL_API_GET_INIT_DATA;
  }


  @Override
  public void pack(Packet packet) {
    super.pack(packet);
  }

  @Override
  public void unpack(Packet packet){
    this.version = packet.getByte();
    this.capabilities = packet.getByte();
    int bitmaskSize = packet.getByte();
    byte[] bitmask = new byte[bitmaskSize];
    packet.get(bitmask);
    BitSet bitSet = BitSet.valueOf(bitmask);
    bitSet.stream().forEach(value -> nodeList.add(value+1));
    chipType = packet.getByte();
  }

}
