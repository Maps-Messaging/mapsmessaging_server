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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTypes;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.ApplicationMapEncoder;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.HeaderEncoder;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.PropertiesEncoder;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.*;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseMessageTranslator implements MessageTranslator {

  private static final String AMQP_TYPE = "amqpType";
  private static final String STRING_TYPE = "string";
  private static final String BYTE_ARRAY_TYPE = "byteArray";
  private static final String DATA_TYPE = "data";

  @Override
  public @NonNull @NotNull MessageBuilder decode(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull org.apache.qpid.proton.message.Message protonMessage) {
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    messageBuilder.setContentType(protonMessage.getContentType());

    HeaderEncoder.unpackHeader(messageBuilder, protonMessage.getHeader());
    Properties props = protonMessage.getProperties();
    if (props != null) {
      PropertiesEncoder.unpackProperties(props, dataMap, messageBuilder);
    }
    ApplicationMapEncoder.unpackApplicationMap(dataMap, protonMessage);
    messageBuilder.setDataMap(dataMap);

    Map<String, String> meta = messageBuilder.getMeta();
    if (meta == null) {
      meta = new LinkedHashMap<>();
      messageBuilder.setMeta(meta);
    }
    meta.put("type", "" + getType());

    Section section = protonMessage.getBody();
    if (section instanceof AmqpValue value) {
      Object val = value.getValue();
      byte[] buf;
      if (val instanceof String str) {
        buf = str.getBytes();
        meta.put(AMQP_TYPE, STRING_TYPE);
      } else if (val instanceof byte[] buffer) {
        buf = buffer;
        meta.put(AMQP_TYPE, BYTE_ARRAY_TYPE);
      } else {
        buf = new byte[0];
      }
      messageBuilder.setOpaqueData(buf);
    } else if (section instanceof Data data) {
      messageBuilder.setOpaqueData(data.getValue().getArray());
      meta.put(AMQP_TYPE, DATA_TYPE);
    }
    return messageBuilder;
  }

  @Override
  public @NonNull @NotNull Message encode(@NonNull @NotNull io.mapsmessaging.api.message.Message message) {
    Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();

    Header header = new Header();
    if (HeaderEncoder.packHeader(message, header)) {
      protonMessage.setHeader(header);
    }

    org.apache.qpid.proton.amqp.messaging.Properties properties = new org.apache.qpid.proton.amqp.messaging.Properties();
    PropertiesEncoder.packProperties(properties, message);
    protonMessage.setProperties(properties);
    ApplicationMapEncoder.packApplicationMap(message, protonMessage);

    Map<Symbol, Object> map = new LinkedHashMap<>();
    Symbol type = Symbol.getSymbol("x-opt-jms-msg-type");
    map.put(type, getType());
    MessageAnnotations annotations = new MessageAnnotations(map);
    protonMessage.setMessageAnnotations(annotations);

    protonMessage.setContentType(message.getContentType());
    if (message.getOpaqueData() != null) {
      String encoding = message.getMeta().get(AMQP_TYPE);
      if (encoding == null) {
        encoding = DATA_TYPE;
      }
      switch (encoding) {
        case STRING_TYPE:
          protonMessage.setBody(new AmqpValue(new String(message.getOpaqueData())));
          break;
        case BYTE_ARRAY_TYPE:
          protonMessage.setBody(new AmqpValue(message.getOpaqueData()));
          break;
        case DATA_TYPE:
        default:
          protonMessage.setBody(new Data(new Binary(message.getOpaqueData())));
          break;
      }
    }
    return protonMessage;
  }

  protected byte getType() {
    return (byte) MessageTypes.MESSAGE.getValue();
  }
}
