package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SHA384PacketSecurity extends HmacPacketSecurity {

  public SHA384PacketSecurity() {
  }

  protected SHA384PacketSecurity(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    super(stamper, key);
  }

  @Override
  public int size() {
    return 48; // 384 bits
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    return new SHA384PacketSecurity(stamper, key);
  }

  @Override
  public String getName() {
    return "HmacSHA384";
  }

}