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

package io.mapsmessaging.network.protocol.transformation;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.transformation.internal.MetaRouteHandler;

import java.nio.ByteBuffer;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.MESSAGE_TRANSFORMATION_EXCEPTION;

public class MessageBinaryTransformation implements ProtocolMessageTransformation {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public String getName() {
    return "Message-Raw";
  }

  @Override
  public int getId() {
    return 3;
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
        int test = buffer.get() & 0xff;
        if (test != 0x81) {
          return; // not known
        }
        int bufferCount = buffer.get();
        ByteBuffer[] buffers = new ByteBuffer[bufferCount];
        int[] bufferSizes = new int[bufferCount];
        for (int x = 0; x < bufferCount; ++x) {
          bufferSizes[x] = buffer.getInt();
        }
        for (int x = 0; x < bufferCount; ++x) {
          buffer.limit(buffer.position() + bufferSizes[x]); // Set limit to the end of the slice
          buffers[x] = ByteBuffer.allocate(bufferSizes[x]);
          buffers[x].put(buffer);
          buffers[x].flip();
        }


        Message message = MessageFactory.getInstance().unpack(buffers);

        Map<String, String> current = messageBuilder.getMeta();
        if (current != null && message.getMeta() != null) {
          current.putAll(message.getMeta());
        } else {
          current = message.getMeta();
        }

        messageBuilder
            .setMeta(current)
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
      logger.log(MESSAGE_TRANSFORMATION_EXCEPTION, e);
    }
  }

  @Override
  public Message outgoing(Message message, String destinationName) {
    if (!destinationName.startsWith("$")) {
      try {
        ByteBuffer[] data = MessageFactory.getInstance().pack(message, MetaRouteHandler.updateRoute(message.getMeta(), message.getCreation()));
        ByteBuffer header = ByteBuffer.allocate(2 + (data.length)*4);
        header.put((byte)0x81);
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
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(response);
        return messageBuilder.build();
      } catch (Exception e) {
        logger.log(MESSAGE_TRANSFORMATION_EXCEPTION, e);
      }
    }
    return message;
  }

}
