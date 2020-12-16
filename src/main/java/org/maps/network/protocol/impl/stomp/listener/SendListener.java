/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.Transaction;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.api.message.TypedData;
import org.maps.network.protocol.impl.stomp.frames.ClientFrame;
import org.maps.network.protocol.impl.stomp.frames.Error;
import org.maps.network.protocol.impl.stomp.frames.Send;
import org.maps.network.protocol.impl.stomp.state.StateEngine;

public class SendListener implements ClientFrameListener {

  public void frameEvent(ClientFrame frame, StateEngine engine, boolean endOfBuffer) throws IOException {
    Send send = (Send) frame;
    Map<String, TypedData> dataMap = new HashMap<>();
    for (Map.Entry<String, String> entry : send.getHeader().entrySet()) {
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "STOMP");
    metaData.put("version", engine.getProtocol().getVersion());
    metaData.put("time_ms", "" + System.currentTimeMillis());
    metaData.put("sessionId", engine.getSession().getName());

    MessageBuilder mb = new MessageBuilder();
    Message message = mb.setDataMap(dataMap)
        .setPriority(Priority.getInstance(send.getPriority()))
        .setOpaqueData(send.getData())
        .setMeta(metaData)
        .setQoS(QualityOfService.AT_LEAST_ONCE)
        .setTransformation(engine.getProtocol().getTransformation())
        .setDelayed(send.getDelay())
        .setMessageExpiryInterval(send.getExpiry(),  TimeUnit.SECONDS)
        .build();

    Destination destination = engine.getSession().findDestination(send.getDestination());
    if(destination != null) {
      if (send.getTransaction() != null) {
        Transaction transaction = engine.getSession().getTransaction(send.getTransaction());
        if (transaction == null) {
          Error error = new Error();
          error.setReceipt(send.getReceipt());
          error.setContent(("No known transaction found " + send.getTransaction()).getBytes());
          error.setContentType("text/plain");
          send.setReceipt(null);
        } else {
          transaction.add(destination, message);
        }
      } else {
        destination.storeMessage(message);
      }
    }
  }
}
