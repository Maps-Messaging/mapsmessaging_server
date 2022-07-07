package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SHA512PacketSecurity extends HmacPacketSecurity {

  public SHA512PacketSecurity() {
  }

  protected SHA512PacketSecurity(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    super(stamper, key);
  }

  @Override
  public int size() {
    return 64; // 512 bits
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    return new SHA512PacketSecurity(stamper, key);
  }

  @Override
  public String getName() {
    return "HmacSHA512";
  }

}