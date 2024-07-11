/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.transformation;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.internal.MetaRouteHandler;
import java.nio.ByteBuffer;
import java.util.Map;

public class MessageBinaryTransformation implements ProtocolMessageTransformation {

  @Override
  public String getName() {
    return "Message-Raw";
  }

  @Override
  public String getDescription() {
    return "Transforms messages to an internal binary payload and vice versa";
  }

  @Override
  public void incoming(MessageBuilder messageBuilder) {
    try {
      byte[] opaqueData = messageBuilder.getOpaqueData();
      if (opaqueData != null) {
        ByteBuffer buffer = ByteBuffer.wrap(opaqueData);
        int bufferCount = buffer.get();
        ByteBuffer[] buffers = new ByteBuffer[bufferCount];
        for(int x = 0; x < bufferCount; ++x) {
          buffers[x] = ByteBuffer.allocate(buffer.getInt());
          buffers[x].put(buffer);
        }
        Message message = MessageFactory.getInstance().unpack(buffers);

        Map<String, String> current = messageBuilder.getMeta();
        if (current != null && message.getMeta() != null) {
          current.putAll(message.getMeta());
        } else {
          current = message.getMeta();
        }

        messageBuilder.setMeta( MetaRouteHandler.updateRoute(current, message.getCreation()))
            .setDataMap(message.getDataMap())
            .setOpaqueData(message.getOpaqueData())
            .setContentType(message.getContentType())
            .setResponseTopic(message.getResponseTopic())
            .setPriority(message.getPriority())
            .setRetain(message.isRetain())
            .setTransformation(null)
            .setDelayed(message.getDelayed())
            .setSchemaId(message.getSchemaId());

      }
    } catch (Exception e) {
      // Log the exception and handle it as needed
      e.printStackTrace();
    }
  }

  @Override
  public byte[] outgoing(Message message, String destinationName) {
    if (!destinationName.startsWith("$")) {
      try {
        ByteBuffer[] data = MessageFactory.getInstance().pack(message);
        ByteBuffer header = ByteBuffer.allocate(1 + (data.length)*4);
        header.put((byte) data.length);
        long totalSize = header.remaining();
        for(ByteBuffer b: data){
          int len = b.remaining();
          header.putInt(len);
          totalSize+=len;
        }
        byte[] response = new byte[(int)totalSize + header.capacity()];
        header.flip();
        header.get(response, 0, header.remaining());
        int pos = header.capacity();
        for(ByteBuffer b: data){
          int len = b.remaining();
          b.get(response, pos, len);
          pos += len;
        }
        return response;
      } catch (Exception e) {
        e.printStackTrace();
        // Log the exception and handle it as needed
      }
    }
    return message.getOpaqueData();
  }

}
