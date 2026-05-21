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

package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.FailureReason;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
import io.mapsmessaging.network.io.security.VerificationResult;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public abstract class HmacPacketSecurity implements PacketIntegrity {

  private final SignatureManager stamper;
  private Mac mac;

  protected HmacPacketSecurity() {
    stamper = new AppenderSignatureManager();
  }

  protected HmacPacketSecurity(SignatureManager stamper, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
    this.stamper = stamper;
    SecretKeySpec secretKeySpec = new SecretKeySpec(key, getName());
    mac = Mac.getInstance(getName());
    mac.init(secretKeySpec);
  }

  @Override
  public void reset() {
    if (mac != null) {
      mac.reset();
    }
  }

  @Override
  public VerificationResult verify(Packet packet) {
    if (packet == null) {
      return VerificationResult.fail(FailureReason.PACKET_NULL, getName(), size(), 0, 0, 0);
    }

    if (mac == null) {
      return VerificationResult.fail(FailureReason.NOT_INITIALISED, getName(), size(), packet.limit(), 0, packet.limit());
    }

    int packetLength = packet.limit();
    int signatureSize = size();

    if (packetLength < signatureSize) {
      return VerificationResult.fail(FailureReason.PACKET_TOO_SHORT, getName(), signatureSize, packetLength, 0, packetLength);
    }

    try {
      // Hash payload-only (exclude trailing signature)
      mac.update(stamper.getData(packet, signatureSize).getRawBuffer());
      byte[] computed = mac.doFinal();
      reset();

      byte[] signature = new byte[computed.length];
      signature = stamper.getSignature(packet, signature);

      boolean ok = constantTimeEquals(signature, computed);
      if (!ok) {
        return VerificationResult.fail(FailureReason.SIGNATURE_MISMATCH, getName(), signatureSize, packetLength, 0, packetLength);
      }

      // ONION UNWRAP: strip signature from packet (old behavior)
      int newLimit = packetLength - computed.length;
      packet.limit(newLimit);
      packet.position(newLimit);

      return VerificationResult.ok(getName(), signatureSize, packetLength, 0, packetLength);
    } catch (IndexOutOfBoundsException e) {
      reset();
      return VerificationResult.fail(FailureReason.SIGNATURE_MISSING, getName(), signatureSize, packetLength, 0, packetLength);
    } catch (RuntimeException e) {
      reset();
      return VerificationResult.error(getName(), signatureSize, packetLength, 0, packetLength, e);
    }
  }



  @Override
  public boolean isSecure(Packet packet) {
    return verify(packet).isValid();
  }

  @Override
  public Packet secure(Packet packet) {
    mac.update(packet.getRawBuffer());
    return updatePacket(packet);
  }

  private Packet updatePacket(Packet packet) {
    byte[] computed = mac.doFinal();
    reset();
    return stamper.setSignature(packet, computed);
  }

  private static boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.length != b.length) {
      return false;
    }
    int diff = 0;
    for (int index = 0; index < a.length; index++) {
      diff |= (a[index] ^ b[index]);
    }
    return diff == 0;
  }
}
