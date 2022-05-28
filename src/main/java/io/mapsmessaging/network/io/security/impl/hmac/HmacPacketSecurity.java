package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class HmacPacketSecurity implements PacketIntegrity {

  private final SignatureManager stamper;
  private Mac mac;

  protected HmacPacketSecurity(){
    stamper = new AppenderSignatureManager();
  }

  protected HmacPacketSecurity(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    this.stamper = stamper;
    SecretKeySpec secretKeySpec = new SecretKeySpec(key, getName());
    mac = Mac.getInstance(getName());
    mac.init(secretKeySpec);
  }

  public void reset(){
    mac.reset();
  }

  public abstract int size();

  @Override
  public Packet secure(Packet packet, int offset, int length) {
    byte[] tmp = packet.getRawBuffer().array();
    mac.update(tmp, offset, length);
    return updatePacket(packet);
  }

  public boolean isSecure(Packet packet) {
    mac.update(stamper.getData(packet, size()).getRawBuffer());
    return validatePacket(packet);
  }

  @Override
  public boolean isSecure(Packet packet, int offset, int length) {
    byte[] tmp = packet.getRawBuffer().array();
    mac.update(tmp, offset, length);
    return validatePacket(packet);
  }

  @Override
  public Packet secure(Packet packet) {
    mac.update(packet.getRawBuffer());
    return updatePacket(packet);
  }

  private boolean validatePacket(Packet packet){
    byte[] computed = mac.doFinal();
    reset();
    byte[] signature = new byte[computed.length];
    signature = stamper.getSignature(packet, signature);
    for(int x=0;x<computed.length;x++){
      if(signature[x] != computed[x]){
        return false;
      }
    }
    packet.limit(packet.limit()-computed.length);
    packet.position(packet.limit());
    return true;
  }

  private Packet updatePacket(Packet packet){
    byte[] computed = mac.doFinal();
    reset();
    return stamper.setSignature(packet, computed);
  }
}