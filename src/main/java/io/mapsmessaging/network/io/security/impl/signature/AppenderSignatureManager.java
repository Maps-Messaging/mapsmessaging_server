package io.mapsmessaging.network.io.security.impl.signature;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.SignatureManager;

public class AppenderSignatureManager implements SignatureManager {
  public AppenderSignatureManager(){
    // Required to be loaded
  }

  @Override
  public byte[] getSignature(Packet packet, byte[] signature) {
    int endPos = packet.limit();
    packet.getRawBuffer().limit(endPos+signature.length);
    packet.position(endPos);
    packet.getRawBuffer().get(signature);
    packet.flip();
    return signature;
  }

  @Override
  public Packet setSignature(Packet packet, byte[] signature) {
    int endPos = packet.limit();
    int newEnd = endPos+signature.length;
    if(packet.capacity() < newEnd){
      Packet p = new Packet(newEnd, false);
      p.setFromAddress(packet.getFromAddress());
      packet.position(0);
      p.put(packet);
      packet = p;
    }
    packet.getRawBuffer().limit(endPos+signature.length);
    packet.position(endPos);
    packet.getRawBuffer().put(signature);
    packet.flip();
    return packet;
  }

  @Override
  public Packet getData(Packet packet, int size) {
    int resetLimit = packet.limit() - size;
    packet.getRawBuffer().limit(packet.limit()-size);
    return packet;
  }

  public String toString(){
    return "Appender";
  }
}
