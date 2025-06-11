/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.security.impl.checksum;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.zip.Checksum;

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
    byte[] crc32 = new byte[size()]; //32 bit CRC
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
    byte[] crc32 = new byte[size()]; //32 bit CRC
    for (int x = 0; x < 4; x++) {
      crc32[x] = (byte) ((crcL >> x) & 0xff);
    }
    return stamper.setSignature(packet, crc32);
  }

  public void reset() {
  }
}
