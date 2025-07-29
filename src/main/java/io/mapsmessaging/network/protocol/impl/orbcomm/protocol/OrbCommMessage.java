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

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.transformation.MessageBinaryTransformation;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
public class OrbCommMessage {

  private final MessageBinaryTransformation transformation = new MessageBinaryTransformation();

  private String namespace;
  private Message message;

  public OrbCommMessage(MessageEvent event) {
    namespace = event.getDestinationName();
    message = event.getMessage();
  }

  public OrbCommMessage(byte[] incomingPackedMessage) {
    unpackFromReceived(incomingPackedMessage);
  }

  public byte[] packToSend(){
    byte[] outgoingPackedMessage = transformation.outgoing(message, namespace);
    byte[] namespaceBytes = namespace.getBytes(StandardCharsets.UTF_8);
    ByteBuffer header = ByteBuffer.allocate(namespaceBytes.length + 4 + outgoingPackedMessage.length);
    header.putInt(namespaceBytes.length);
    header.put(namespaceBytes);
    header.put(outgoingPackedMessage);
    return header.array();
  }

  private void unpackFromReceived(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);

    // 1. Read the 4-byte namespace length
    int namespaceLength = buffer.getInt();

    // 2. Extract and decode the namespace
    byte[] namespaceBytes = new byte[namespaceLength];
    buffer.get(namespaceBytes);
    namespace = new String(namespaceBytes, StandardCharsets.UTF_8);

    // 3. Extract the remaining bytes as the message
    byte[] messageBytes = new byte[buffer.remaining()];
    buffer.get(messageBytes);
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(messageBytes);
    transformation.incoming(messageBuilder);
    message = messageBuilder.build();
  }

}
