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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.transformation.internal.MessageLoader;
import io.mapsmessaging.network.protocol.transformation.internal.MessagePacker;

import static io.mapsmessaging.logging.ServerLogMessages.MESSAGE_TRANSFORMATION_EXCEPTION;

public class MessageJsonTransformation implements ProtocolMessageTransformation {

  private static final ObjectMapper objectMapper = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // Configure the ObjectMapper as needed
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }

  @Override
  public int getId() {
    return 5;
  }


  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public String getName() {
    return "Message-JSON";
  }

  @Override
  public String getDescription() {
    return "Transforms a message into an internal JSON payload and vice versa";
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
      logger.log(MESSAGE_TRANSFORMATION_EXCEPTION, e);
    }
  }

  @Override
  public Message outgoing(Message message, String destinationName) {
    if (!destinationName.startsWith("$")) {
      try {
        byte[] data = objectMapper.writeValueAsBytes(new MessagePacker(message));
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(data);
        return messageBuilder.build();
      } catch (Exception e) {
        logger.log(MESSAGE_TRANSFORMATION_EXCEPTION, e);
      }
    }
    return message;
  }

}
