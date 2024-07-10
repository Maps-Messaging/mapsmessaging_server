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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.internal.MessageLoader;
import io.mapsmessaging.network.protocol.transformation.internal.MessagePacker;

public class MessageJsonTransformation implements ProtocolMessageTransformation {

  private static final ObjectMapper objectMapper = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // Configure the ObjectMapper as needed
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }

  @Override
  public String getName() {
    return "Message-JSON";
  }

  @Override
  public String getDescription() {
    return "Transforms MessageBuilder to JSON payload and vice versa";
  }

  @Override
  public void incoming(MessageBuilder messageBuilder) {
    try {
      byte[] opaqueData = messageBuilder.getOpaqueData();
      if (opaqueData != null) {
        String json = new String(opaqueData);
        MessageLoader message = objectMapper.readValue(json, MessageLoader.class);
        message.load(messageBuilder);
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
        return objectMapper.writeValueAsBytes(new MessagePacker(message));
      } catch (Exception e) {
        // Log the exception and handle it as needed
        e.printStackTrace();
      }
    }
    return message.getOpaqueData();
  }

}
