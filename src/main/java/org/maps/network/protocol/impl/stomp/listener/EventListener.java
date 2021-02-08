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
import org.maps.network.protocol.impl.stomp.frames.Error;
import org.maps.network.protocol.impl.stomp.frames.Event;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.state.StateEngine;

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
  protected abstract void processEvent( StateEngine engine, Event event, Message message) throws IOException;
}
