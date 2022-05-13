package io.mapsmessaging.network.io.security.impl.checksum;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Crc32PacketSecurity extends ChecksumPacketSecurity {

  public Crc32PacketSecurity(){
  }

  protected Crc32PacketSecurity(SignatureManager stamper){
    super(stamper);
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper) {
    return new Crc32PacketSecurity(stamper);
  }

  @Override
  public String getName() {
    return "CRC32";
  }

  protected Checksum getChecksum(){
    return new CRC32();
  }

}
