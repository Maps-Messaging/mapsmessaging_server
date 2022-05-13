package io.mapsmessaging.network.io.security.impl.checksum;

import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class Crc32CPacketSecurity extends ChecksumPacketSecurity {

  public Crc32CPacketSecurity(){
  }

  protected Crc32CPacketSecurity(SignatureManager stamper){
    super(stamper);
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper) {
    return new Crc32CPacketSecurity(stamper);
  }


  @Override
  public String getName() {
    return "CRC32C";
  }

  protected Checksum getChecksum(){
    return new CRC32C();
  }

}
