/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.io.security.FailureReason;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import io.mapsmessaging.network.io.security.VerificationResult;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

public abstract class ChecksumPacketSecurity implements PacketIntegrity {

  private static final int CHECKSUM_SIZE_BYTES = 4;

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

  @Override
  public int size() {
    return CHECKSUM_SIZE_BYTES;
  }

  protected abstract Checksum getChecksum();

  @Override
  public VerificationResult verify(Packet packet) {
    if (packet == null) {
      return VerificationResult.fail(FailureReason.PACKET_NULL, getName(), size(), 0, 0, 0);
    }

    int packetLength = packet.limit();
    int signatureSize = size();

    if (packetLength < signatureSize) {
      return VerificationResult.fail(FailureReason.PACKET_TOO_SHORT, getName(), signatureSize, packetLength, 0, packetLength);
    }

    try {
      Checksum checksum = getChecksum();

      ByteBuffer dataBuffer = stamper.getData(packet, signatureSize).getRawBuffer();
      checksum.update(dataBuffer);

      boolean ok = validatePacket(checksum, packet);
      if (!ok) {
        return VerificationResult.fail(FailureReason.SIGNATURE_MISMATCH, getName(), signatureSize, packetLength, 0, packetLength);
      }

      // ONION UNWRAP: strip CRC/signature from the end (old behavior)
      int newLimit = packetLength - signatureSize;
      packet.limit(newLimit);
      packet.position(newLimit);

      return VerificationResult.ok(getName(), signatureSize, packetLength, 0, packetLength);
    } catch (IndexOutOfBoundsException e) {
      return VerificationResult.fail(FailureReason.SIGNATURE_MISSING, getName(), signatureSize, packetLength, 0, packetLength);
    } catch (RuntimeException e) {
      return VerificationResult.error(getName(), signatureSize, packetLength, 0, packetLength, e);
    }
  }

  @Override
  public Packet secure(Packet packet) {
    Checksum checksum = getChecksum();
    checksum.update(packet.getRawBuffer());
    return updatePacket(checksum, packet);
  }

  @Override
  public boolean isSecure(Packet packet) {
    return verify(packet).isValid();
  }

  private boolean validatePacket(Checksum checksum, Packet packet) {
    long checksumValue = checksum.getValue();
    byte[] signature = new byte[size()];
    signature = stamper.getSignature(packet, signature);

    for (int index = 0; index < CHECKSUM_SIZE_BYTES; index++) {
      byte expected = (byte) ((checksumValue >> (index * 8)) & 0xFF);
      if (signature[index] != expected) {
        return false;
      }
    }
    return true;
  }

  private Packet updatePacket(Checksum checksum, Packet packet) {
    long checksumValue = checksum.getValue();
    byte[] signature = new byte[size()];
    for (int index = 0; index < CHECKSUM_SIZE_BYTES; index++) {
      signature[index] = (byte) ((checksumValue >> (index * 8)) & 0xFF);
    }
    return stamper.setSignature(packet, signature);
  }

  @Override
  public void reset() {
  }
}
