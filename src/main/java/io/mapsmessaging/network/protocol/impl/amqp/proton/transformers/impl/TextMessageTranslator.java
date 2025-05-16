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
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTypes;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;

public class TextMessageTranslator extends BaseMessageTranslator {

  @Override
  public @NonNull @NotNull MessageBuilder decode(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull org.apache.qpid.proton.message.Message protonMessage) {
    super.decode(messageBuilder, protonMessage);
    Section body = protonMessage.getBody();
    if (body instanceof AmqpValue) {
      AmqpValue amqpBody = (AmqpValue) body;
      Object data = amqpBody.getValue();
      if (data instanceof String) {
        messageBuilder.setOpaqueData(data.toString().getBytes());
      }
    }
    return messageBuilder;
  }

  @Override
  public @NonNull @NotNull Message encode(@NonNull @NotNull io.mapsmessaging.api.message.Message message) {
    Message protonMessage = super.encode(message);
    if (message.getOpaqueData() != null) {
      AmqpValue body = new AmqpValue(new String(message.getOpaqueData()));
      protonMessage.setBody(body);
    }
    return protonMessage;
  }

  @Override
  protected byte getType() {
    return (byte) MessageTypes.TEXT.getValue();
  }

}
