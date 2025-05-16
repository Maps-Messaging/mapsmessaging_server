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

package io.mapsmessaging.network.protocol.impl.amqp.proton.tasks;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.SubscriptionManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslatorFactory;
import org.apache.qpid.proton.codec.DroppingWritableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Sender;

import java.io.IOException;

public class SendMessageTask extends PacketTask {

  private final Message message;
  private final SubscribedEventManager manager;
  private final SubscriptionManager subscriptions;

  public SendMessageTask(ProtonEngine engine, Message message, SubscribedEventManager manager) {
    super(engine);
    this.message = message;
    this.manager = manager;
    subscriptions = engine.getSubscriptions();
  }

  @Override
  public Boolean call() throws Exception {
    processMessage();
    return true;
  }

  private void processMessage() throws IOException {
    String alias = manager.getContext().getAlias();
    Sender sender = subscriptions.get(alias);
    if (sender != null) {
      byte[] tag = packLong(message.getIdentifier());
      Delivery dlv = sender.delivery(tag);
      dlv.setContext(manager);
      MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(message);
      try {
        org.apache.qpid.proton.message.Message protonMessage = translator.encode(message);
        WritableBuffer sizingBuffer = new DroppingWritableBuffer();
        protonMessage.encode(sizingBuffer);
        byte[] data = new byte[sizingBuffer.position() + 10];
        int size = protonMessage.encode(data, 0, data.length);
        sender.send(data, 0, size);
        sender.advance();
        if (message.isLastMessage()) {
          sender.drained();
        }
      } catch (Exception e) {
        protocol.getLogger().log(ServerLogMessages.AMQP_ENGINE_TRANSPORT_EXCEPTION, e);
      }
    }
    processOutput();
  }


  public byte[] packLong(long value) {
    byte[] buff = new byte[8];
    for (int x = 0; x < buff.length; x++) {
      buff[x] = (byte) ((value >> (8 * x)) & 0xff);
    }
    return buff;
  }

}