package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class BasePacket implements ServerPacket {

  @Override
  public void complete() {

  }

  @Override
  public SocketAddress getFromAddress(){
    return InetSocketAddress.createUnresolved("127.0.0.1", 0);
  }

  protected int computeChecksum(Packet packet){
    int checksum = 0xff;
    for (int x=1;x<packet.position();x++){
      checksum = computeCheckSum(checksum, packet.get(x));
    }
    return checksum;
  }

  protected int computeCheckSum(int csum, int val){
    return csum ^ val;
  }
}
