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

package io.mapsmessaging.network.io.security.impl.signature;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.SignatureManager;

import java.nio.ByteBuffer;

public class PrependerSignatureManager implements SignatureManager {

  public PrependerSignatureManager() {
    // Required to be loaded
  }

  @Override
  public byte[] getSignature(Packet packet, byte[] signature) {
    ByteBuffer buffer = packet.getRawBuffer();
    int pos = buffer.position();
    buffer.position(0);
    packet.getRawBuffer().get( signature);
    buffer.position(pos);
    return signature;
  }

  @Override
  public Packet setSignature(Packet packet, byte[] signature) {
    byte[] tmp = new byte[packet.limit() + signature.length];
    Packet p = new Packet(ByteBuffer.wrap(tmp));
    p.put(signature);
    packet.flip();
    p.put(packet);
    p.flip();
    return p;
  }

  @Override
  public Packet getData(Packet packet, int size) {
    packet.position(size);
    return packet;
  }

  public String toString() {
    return "Prepender";
  }

}
