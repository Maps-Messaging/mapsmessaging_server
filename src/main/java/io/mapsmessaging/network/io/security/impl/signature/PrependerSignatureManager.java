package io.mapsmessaging.network.io.security.impl.signature;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.nio.ByteBuffer;

public class PrependerSignatureManager implements SignatureManager {

  public PrependerSignatureManager() {
    // Required to be loaded
  }

  @Override
  public byte[] getSignature(Packet packet, byte[] signature) {
    packet.getRawBuffer().get(0, signature);
    return signature;
  }

  @Override
  public Packet setSignature(Packet packet, byte[] signature) {
    byte[] tmp = new byte[packet.limit() + signature.length];
    Packet p = new Packet(ByteBuffer.wrap(tmp));
    p.put(signature);
    packet.flip();
    p.put(packet);
    p.flip();
    return p;
  }

  @Override
  public Packet getData(Packet packet, int size) {
    packet.position(size);
    return packet;
  }

  public String toString() {
    return "Prepender";
  }

}
