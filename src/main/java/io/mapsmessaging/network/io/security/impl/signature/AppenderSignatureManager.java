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

public class AppenderSignatureManager implements SignatureManager {

  public AppenderSignatureManager() {
    // Required to be loaded
  }

  @Override
  public byte[] getSignature(Packet packet, byte[] signature) {
    int endPos = packet.limit();
    packet.getRawBuffer().limit(endPos + signature.length);
    packet.position(endPos);
    packet.getRawBuffer().get(signature);
    packet.flip();
    return signature;
  }

  @Override
  public Packet setSignature(Packet packet, byte[] signature) {
    int endPos = packet.limit();
    int newEnd = endPos + signature.length;
    if (packet.capacity() < newEnd) {
      Packet p = new Packet(newEnd, false);
      p.setFromAddress(packet.getFromAddress());
      packet.position(0);
      p.put(packet);
      packet = p;
    }
    packet.getRawBuffer().limit(endPos + signature.length);
    packet.position(endPos);
    packet.getRawBuffer().put(signature);
    packet.flip();
    return packet;
  }

  @Override
  public Packet getData(Packet packet, int size) {
    packet.getRawBuffer().limit(packet.limit() - size);
    return packet;
  }

  public String toString() {
    return "Appender";
  }
}
