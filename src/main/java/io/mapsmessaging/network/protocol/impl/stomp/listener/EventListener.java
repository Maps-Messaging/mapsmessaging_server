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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Event;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class EventListener implements FrameListener {

  public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer) throws IOException {
    Event event = (Event) frame;
    Map<String, TypedData> dataMap = new HashMap<>();
    for (Map.Entry<String, String> entry : event.getHeader().entrySet()) {
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "STOMP");
    metaData.put("version", engine.getProtocol().getVersion());
    metaData.put("sessionId", engine.getSession().getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());
    MessageBuilder mb = new MessageBuilder();
    mb.setDataMap(dataMap)
        .setPriority(Priority.getInstance(event.getPriority()))
        .setOpaqueData(event.getData())
        .setMeta(metaData)
        .setTransformation(engine.getProtocol().getTransformation())
        .setDelayed(event.getDelay())
        .setMessageExpiryInterval(event.getExpiry(), TimeUnit.SECONDS)
        .build();
    Message message = MessageOverrides.createMessageBuilder(engine.getProtocol().getProtocolConfig().getMessageDefaults(), mb).build();
    processEvent(engine, event, message);
  }

  protected void handleMessageStoreToDestination(Destination destination, SessionState engine, Event event, Message message) throws IOException {
    if (destination != null) {
      if (event.getTransaction() != null) {
        Transaction transaction = engine.getSession().getTransaction(event.getTransaction());
        if (transaction == null) {
          Error error = new Error();
          error.setReceipt(event.getReceipt());
          error.setContent(("No known transaction found " + event.getTransaction()).getBytes());
          error.setContentType("text/plain");
          event.setReceipt(null);
        } else {
          transaction.add(destination, message);
        }
      } else {
        destination.storeMessage(message);
      }
    }
  }

  protected abstract void processEvent(SessionState engine, Event event, Message message) throws IOException;
}
