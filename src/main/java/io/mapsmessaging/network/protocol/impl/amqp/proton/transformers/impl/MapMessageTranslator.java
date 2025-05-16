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
import io.mapsmessaging.api.message.TypedData.TYPE;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTypes;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapMessageTranslator extends BaseMessageTranslator {

  @Override
  public @NonNull @NotNull MessageBuilder decode(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull org.apache.qpid.proton.message.Message protonMessage) {
    super.decode(messageBuilder, protonMessage);
    Section body = protonMessage.getBody();
    if (body instanceof AmqpValue) {
      AmqpValue amqpBody = (AmqpValue) body;
      Object data = amqpBody.getValue();
      Map<String, TypedData> dataMap = messageBuilder.getDataMap();
      if (dataMap == null) {
        dataMap = new LinkedHashMap<>();
        messageBuilder.setDataMap(dataMap);
      }
      if (data instanceof LinkedHashMap) {
        LinkedHashMap<String, Object> map = (LinkedHashMap) data;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          Object val = entry.getValue();
          if (val instanceof Binary) {
            Binary binary = (Binary) val;
            dataMap.put(entry.getKey(), new TypedData(binary.getArray()));
          } else {
            dataMap.put(entry.getKey(), new TypedData(val));
          }
        }
      }
    }
    return messageBuilder;
  }

  @Override
  public @NonNull @NotNull Message encode(@NonNull @NotNull io.mapsmessaging.api.message.Message message) {
    Message protonMessage = super.encode(message);
    Map<String, Object> map = new LinkedHashMap<>();
    Map<String, TypedData> dataMap = message.getDataMap();
    for (Map.Entry<String, TypedData> entry : dataMap.entrySet()) {
      if (entry.getValue().getType().equals(TYPE.BYTE_ARRAY)) {
        byte[] bytes = (byte[]) entry.getValue().getData();
        Binary bin = new Binary(bytes);
        map.put(entry.getKey(), bin);
      } else {
        map.put(entry.getKey(), entry.getValue().getData());
      }
    }
    AmqpValue amqpBody = new AmqpValue(map);
    protonMessage.setBody(amqpBody);
    return protonMessage;
  }

  @Override
  protected byte getType() {
    return (byte) MessageTypes.MAP.getValue();
  }

}
