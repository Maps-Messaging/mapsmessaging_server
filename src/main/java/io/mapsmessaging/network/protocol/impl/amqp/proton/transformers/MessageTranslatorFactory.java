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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers;

import io.mapsmessaging.api.message.Message;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MessageTranslatorFactory {

  public static @NonNull @NotNull MessageTranslator getMessageTranslator(@Nullable MessageAnnotations annotations) {
    MessageTypes messageType = MessageTypes.MESSAGE;

    Map<Symbol, Object> maps;
    if (annotations != null) {
      maps = annotations.getValue();
      Object type = maps.get(Symbol.getSymbol("x-opt-jms-msg-type"));
      if (type instanceof Byte) {
        messageType = MessageTypes.getInstance((Byte) type);
      }
    }
    return messageType.getMessageTranslator();
  }

  public static @NonNull @NotNull MessageTranslator getMessageTranslator(@NonNull @NotNull Message message) {
    MessageTypes messageType = MessageTypes.BYTES;
    Map<String, String> metaData = message.getMeta();
    if (metaData != null && metaData.containsKey("type")) {
      String val = metaData.get("type");
      byte lookup = Byte.decode(val);
      messageType = MessageTypes.getInstance(lookup);
    }
    return messageType.getMessageTranslator();
  }

  private MessageTranslatorFactory() {
  }

}
