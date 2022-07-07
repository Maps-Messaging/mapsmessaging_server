package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SHA224PacketSecurity extends HmacPacketSecurity {

  public SHA224PacketSecurity() {
  }

  protected SHA224PacketSecurity(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    super(stamper, key);
  }

  @Override
  public int size() {
    return 28; // 224 bits
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    return new SHA224PacketSecurity(stamper, key);
  }

  @Override
  public String getName() {
    return "HmacSHA224";
  }

}
