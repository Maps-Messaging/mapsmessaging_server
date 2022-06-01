package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import static io.mapsmessaging.network.protocol.impl.z_wave.Constants.SOF;

import io.mapsmessaging.network.io.Packet;

public abstract class DataPacket extends BasePacket{

  private int computedChecksum;
  private int checksum;

  public DataPacket(){}

  public DataPacket(Packet packet){
    int len = packet.get();
    int type = packet.get();
    byte[] data = new byte[packet.available()-1];
    packet.get(data);
    checksum = (packet.get() & 0xff);

    int csum = computeCheckSum(0xff, (len&0xff));
    csum = computeCheckSum(csum, type);
    for(byte b:data){
      csum = computeCheckSum(csum, (b&0xff));
    }
    dumpData(data);
    computedChecksum = csum;

  }

  private void dumpData(byte[] data){
    StringBuilder sb = new StringBuilder("Len:");
    sb.append(data.length).append(" [");
    for(byte b:data){
      int val = (b &0xff);
      sb.append(Integer.toHexString(val)).append(",");
    }
    sb.append("]");
    System.err.println(sb);
  }

  public boolean isValid(){
    return checksum == computedChecksum;
  }


  public abstract int getType();

  public abstract int packData(Packet packet);

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(SOF);
    packet.putByte(0); // Length - hold
    packet.putByte(getType());
    packData(packet);
    packet.putByte(computeChecksum(packet));
    packet.put(1, ( byte) (packet.position()-1 & 0xff));
    return packet.position();
  }
}