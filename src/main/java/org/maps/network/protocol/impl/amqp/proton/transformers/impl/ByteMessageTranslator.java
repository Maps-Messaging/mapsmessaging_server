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

package org.maps.network.protocol.impl.amqp.proton.transformers.impl;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.MessageBuilder;
import org.maps.network.protocol.impl.amqp.proton.transformers.MessageTypes;

public class ByteMessageTranslator extends BaseMessageTranslator {

  @Override
  public @NotNull MessageBuilder decode(@NotNull MessageBuilder messageBuilder, @NotNull org.apache.qpid.proton.message.Message protonMessage){
    super.decode(messageBuilder, protonMessage);
    Data data = (Data) protonMessage.getBody();
    messageBuilder.setOpaqueData(data.getValue().getArray());
    return messageBuilder;
  }

  @Override
  public @NotNull Message encode(@NotNull org.maps.messaging.api.message.Message message) {
    Message protonMessage = super.encode(message);
    Data data = new Data(new Binary(message.getOpaqueData()));
    protonMessage.setBody(data);
    return protonMessage;
  }

  @Override
  protected byte getType(){
    return (byte) MessageTypes.BYTES.getValue();
  }

}