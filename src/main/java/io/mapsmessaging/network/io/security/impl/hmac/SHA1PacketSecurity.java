package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SHA1PacketSecurity extends HmacPacketSecurity {

  public SHA1PacketSecurity() {
  }

  protected SHA1PacketSecurity(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    super(stamper, key);
  }

  @Override
  public int size() {
    return 20; // 160 bits
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    return new SHA1PacketSecurity(stamper, key);
  }

  @Override
  public String getName() {
    return "HmacSHA1";
  }
}