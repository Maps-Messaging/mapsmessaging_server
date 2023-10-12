/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTypes;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;

public class ByteMessageTranslator extends BaseMessageTranslator {

  @Override
  public @NonNull @NotNull MessageBuilder decode(@NonNull @NotNull MessageBuilder messageBuilder, @NonNull @NotNull org.apache.qpid.proton.message.Message protonMessage) {
    super.decode(messageBuilder, protonMessage);
    Data data = (Data) protonMessage.getBody();
    messageBuilder.setOpaqueData(data.getValue().getArray());
    return messageBuilder;
  }

  @Override
  public @NonNull @NotNull Message encode(@NonNull @NotNull io.mapsmessaging.api.message.Message message) {
    Message protonMessage = super.encode(message);
    Data data = new Data(new Binary(message.getOpaqueData()));
    protonMessage.setBody(data);
    return protonMessage;
  }

  @Override
  protected byte getType() {
    return (byte) MessageTypes.BYTES.getValue();
  }

}