/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTypes;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.ApplicationMapEncoder;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.HeaderEncoder;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.PropertiesEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;

public class BaseMessageTranslator implements MessageTranslator {

  @Override
  public @NonNull @NotNull MessageBuilder decode(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull org.apache.qpid.proton.message.Message protonMessage){
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    messageBuilder.setContentType(protonMessage.getContentType());

    HeaderEncoder.unpackHeader(messageBuilder,  protonMessage.getHeader());
    PropertiesEncoder.unpackProperties(protonMessage.getProperties(), dataMap, messageBuilder);
    ApplicationMapEncoder.unpackApplicationMap(dataMap, protonMessage);

    messageBuilder.setDataMap(dataMap);

    Map<String, String> meta = messageBuilder.getMeta();
    if(meta == null){
      meta = new LinkedHashMap<>();
      messageBuilder.setMeta(meta);
    }
    meta.put("type", ""+ getType());
    return messageBuilder;
  }

  @Override
  public @NonNull @NotNull Message encode(@NonNull @NotNull io.mapsmessaging.api.message.Message message) {
    Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();

    Header header = new Header();
    if(HeaderEncoder.packHeader(message, header)){
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
    return protonMessage;
  }

  protected byte getType(){
    return (byte)MessageTypes.MESSAGE.getValue();
  }
}
