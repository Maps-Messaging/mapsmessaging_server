/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Event;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.state.StateEngine;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class EventListener implements FrameListener {

  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) throws IOException {
    Event event = (Event) frame;
    Map<String, TypedData> dataMap = new HashMap<>();
    for (Map.Entry<String, String> entry : event.getHeader().entrySet()) {
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "STOMP");
    metaData.put("version", engine.getProtocol().getVersion());
    metaData.put("time_ms", "" + System.currentTimeMillis());
    metaData.put("sessionId", engine.getSession().getName());

    MessageBuilder mb = new MessageBuilder();
    Message message = mb.setDataMap(dataMap)
        .setPriority(Priority.getInstance(event.getPriority()))
        .setOpaqueData(event.getData())
        .setMeta(metaData)
        .setQoS(QualityOfService.AT_LEAST_ONCE)
        .setTransformation(engine.getProtocol().getTransformation())
        .setDelayed(event.getDelay())
        .setMessageExpiryInterval(event.getExpiry(),  TimeUnit.SECONDS)
        .build();
    processEvent(engine, event, message);
  }

  protected void handleMessageStoreToDestination( Destination destination, StateEngine engine, Event event, Message message) throws IOException {
    if(destination != null) {
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

  protected abstract void processEvent( StateEngine engine, Event event, Message message) throws IOException;
}
