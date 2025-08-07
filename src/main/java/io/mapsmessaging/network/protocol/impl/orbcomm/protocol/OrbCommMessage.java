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

package io.mapsmessaging.network.protocol.impl.orbcomm.protocol;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
public class OrbCommMessage {

  private String namespace;
  private byte[] message;

  public OrbCommMessage(String namespace,  byte[] message) {
    this.namespace =namespace;
    this.message = message;
  }

  public OrbCommMessage(byte[] incomingPackedMessage) {
    unpackFromReceived(incomingPackedMessage);
  }

  public byte[] packToSend(){
    byte[] namespaceBytes = namespace.getBytes(StandardCharsets.UTF_8);
    ByteBuffer header = ByteBuffer.allocate(namespaceBytes.length + 4 + message.length);
    header.putInt(namespaceBytes.length);
    header.put(namespaceBytes);
    header.put(message);
    return header.array();
  }

  private void unpackFromReceived(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.get(); // initial byte is 0 for some reason
    // 1. Read the 4-byte namespace length
    int namespaceLength = buffer.getInt();

    // 2. Extract and decode the namespace
    byte[] namespaceBytes = new byte[namespaceLength];
    buffer.get(namespaceBytes);
    namespace = new String(namespaceBytes, StandardCharsets.UTF_8);

    // 3. Extract the remaining bytes as the message
    message = new byte[buffer.remaining()];
    buffer.get(message);
  }

}
