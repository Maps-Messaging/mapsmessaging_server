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
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslatorFactory;
import lombok.NonNull;
import org.apache.qpid.proton.codec.DroppingWritableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.message.Message.Factory;
import org.jetbrains.annotations.NotNull;

public class JMSProtocolMessageTransformation implements ProtocolMessageTransformation {

  public JMSProtocolMessageTransformation() {
    // Used by the java services
  }

  @Override
  public String getName() {
    return "AMQP-JMS";
  }

  @Override
  public String getDescription() {
    return "Implementation of JMS over AMQP messaging as per http://docs.oasis-open.org/amqp-bindmap/jms/v1.0/wd09/amqp-bindmap-jms-v1.0-wd09.html";
  }

  @Override
  public void incoming(@NonNull @NotNull MessageBuilder messageBuilder) {
    org.apache.qpid.proton.message.Message protonMsg = Factory.create();
    byte[] data = messageBuilder.getOpaqueData();
    if (data != null) {
      protonMsg.decode(data, 0, data.length);
      MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(protonMsg.getMessageAnnotations());
      translator.decode(messageBuilder, protonMsg);
    }
  }

  @Override
  public @NonNull byte[] outgoing(@NonNull @NotNull Message message, String destinationName) {

    MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(message);
    org.apache.qpid.proton.message.Message protonMsg = translator.encode(message);

    WritableBuffer sizingBuffer = new DroppingWritableBuffer();
    protonMsg.encode(sizingBuffer);
    byte[] data = new byte[sizingBuffer.position() + 10];
    int size = protonMsg.encode(data, 0, data.length);
    if (size != data.length) {
      byte[] tmp = new byte[size];
      System.arraycopy(data, 0, tmp, 0, size);
      data = tmp;
    }
    return data;
  }
}