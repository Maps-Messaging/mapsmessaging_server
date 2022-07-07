package io.mapsmessaging.network.io.security.impl.checksum;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class Adler32PacketSecurity extends ChecksumPacketSecurity {

  public Adler32PacketSecurity() {
  }

  protected Adler32PacketSecurity(SignatureManager stamper) {
    super(stamper);
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper) {
    return new Adler32PacketSecurity(stamper);
  }

  @Override
  public String getName() {
    return "Adler32";
  }

  protected Checksum getChecksum() {
    return new Adler32();
  }

}
