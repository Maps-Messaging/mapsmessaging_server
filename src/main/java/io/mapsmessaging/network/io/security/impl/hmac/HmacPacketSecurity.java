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

package io.mapsmessaging.network.io.security.impl.hmac;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.SignatureManager;
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

  public void reset() {
    mac.reset();
  }


  @Override
  public Packet secure(Packet packet, int offset, int length) {
    byte[] tmp = packet.getRawBuffer().array();
    mac.update(tmp, offset, length);
    return updatePacket(packet);
  }

  public boolean isSecure(Packet packet) {
    mac.update(stamper.getData(packet, size()).getRawBuffer());
    return validatePacket(packet);
  }

  @Override
  public boolean isSecure(Packet packet, int offset, int length) {
    byte[] tmp = packet.getRawBuffer().array();
    mac.update(tmp, offset, length);
    return validatePacket(packet);
  }

  @Override
  public Packet secure(Packet packet) {
    mac.update(packet.getRawBuffer());
    return updatePacket(packet);
  }

  private boolean validatePacket(Packet packet) {
    byte[] computed = mac.doFinal();
    reset();
    byte[] signature = new byte[computed.length];
    signature = stamper.getSignature(packet, signature);
    for (int x = 0; x < computed.length; x++) {
      if (signature[x] != computed[x]) {
        return false;
      }
    }
    packet.limit(packet.limit() - computed.length);
    packet.position(packet.limit());
    return true;
  }

  private Packet updatePacket(Packet packet) {
    byte[] computed = mac.doFinal();
    reset();
    return stamper.setSignature(packet, computed);
  }
}