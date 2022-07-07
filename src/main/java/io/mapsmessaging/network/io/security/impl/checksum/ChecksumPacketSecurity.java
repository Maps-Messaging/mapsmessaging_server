package io.mapsmessaging.network.io.security.impl.checksum;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import java.util.zip.Checksum;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public abstract class ChecksumPacketSecurity implements PacketIntegrity {

  private final SignatureManager stamper;

  protected ChecksumPacketSecurity() {
    stamper = new AppenderSignatureManager();
  }

  protected ChecksumPacketSecurity(@NotNull @NonNull SignatureManager stamper) {
    this.stamper = stamper;
  }

  @Override
  public PacketIntegrity initialise(SignatureManager stamper, byte[] key) {
    return initialise(stamper);
  }

  public abstract PacketIntegrity initialise(SignatureManager stamper);

  public int size() {
    return 4; // 32 bits
  }

  abstract Checksum getChecksum();

  @Override
  public Packet secure(Packet packet, int offset, int length) {
    Checksum checksum = getChecksum();
    byte[] tmp = packet.getRawBuffer().array();
    checksum.update(tmp, offset, length);
    return updatePacket(checksum, packet);
  }

  public boolean isSecure(Packet packet) {
    Checksum checksum = getChecksum();
    checksum.update(stamper.getData(packet, size()).getRawBuffer());
    return validatePacket(checksum, packet);
  }

  @Override
  public boolean isSecure(Packet packet, int offset, int length) {
    Checksum checksum = getChecksum();
    byte[] tmp = packet.getRawBuffer().array();
    checksum.update(tmp, offset, length);
    return validatePacket(checksum, packet);
  }

  @Override
  public Packet secure(Packet packet) {
    Checksum checksum = getChecksum();
    checksum.update(packet.getRawBuffer());
    return updatePacket(checksum, packet);
  }

  private boolean validatePacket(Checksum checksum, Packet packet) {
    long crcL = checksum.getValue();
    byte[] crc32 = new byte[size()]; //32 bits;
    crc32 = stamper.getSignature(packet, crc32);
    for (int x = 0; x < 4; x++) {
      if (crc32[x] != (byte) ((crcL >> x) & 0xff)) {
        return false;
      }
    }
    return true;
  }

  private Packet updatePacket(Checksum checksum, Packet packet) {
    long crcL = checksum.getValue();
    byte[] crc32 = new byte[size()]; //32 bits;
    for (int x = 0; x < 4; x++) {
      crc32[x] = (byte) ((crcL >> x) & 0xff);
    }
    return stamper.setSignature(packet, crc32);
  }

  public void reset() {
  }
}
