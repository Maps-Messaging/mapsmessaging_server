/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.nats.frames.*;
import io.mapsmessaging.network.protocol.impl.nats.listener.FrameListener;

import java.io.IOException;
import java.util.Map;


public class ConnectedState implements State {

  public void handleFrame(SessionState engine, NatsFrame frame, boolean endOfBuffer) throws IOException {
    FrameListener listener = frame.getListener();
    if (listener != null) {
      try {
        listener.frameEvent(frame, engine, endOfBuffer);
        listener.postFrameHandling(frame, engine);
        frame.complete();
      } catch (NatsProtocolException e) {
        ErrFrame error = new ErrFrame();
        error.setError(e.getMessage());
        if (frame.getReceipt() != null) {
          error.setReceipt(frame.getReceipt());
        }
        engine.send(error);
        frame.setReceipt(null); // its been handled
      }
    } else {
      throw new NatsProtocolException("Unhandled Client Frame received");
    }
  }

  @Override
  public boolean sendMessage(SessionState engine, String destinationName, SubscriptionContext context, Message message, Runnable completionTask) {
    PayloadFrame msg;
    if (engine.isHeaders() && !message.getDataMap().isEmpty()) {
      msg = new HMsgFrame(engine.getMaxBufferSize());
      StringBuilder sb = new StringBuilder("NATS/1.0\r\n");
      for (Map.Entry<String, TypedData> entry : message.getDataMap().entrySet()) {
        sb.append(entry.getKey().replace(" ", "_")).append(": ").append("" + entry.getValue().getData()).append("\r\n");
      }
      sb.append("\r\n");
      ((HMsgFrame)msg).setHeaderBytes(sb.toString().getBytes());
    } else {
      msg =  new MsgFrame(engine.getMaxBufferSize());
    }
    byte[] payloadData = message.getOpaqueData();
    msg.setSubject(mapMqttTopicToNatsSubject(destinationName));
    msg.setSubscriptionId(context.getAlias());
    if (message.getCorrelationData() != null) {
      msg.setReplyTo(new String(message.getCorrelationData()));
    }
    msg.setPayloadSize(payloadData.length);
    msg.setPayload(payloadData);
    return engine.send(msg);
  }

  private String mapMqttTopicToNatsSubject(String mqttTopic) {
    return mqttTopic
        .replace('/', '.')
        .replace('+', '*')
        .replace('#', '>');
  }


  static class MessageCompletionHandler implements CompletionHandler {

    private final Runnable sessionCompletion;

    MessageCompletionHandler(Runnable runnable) {
      sessionCompletion = runnable;
    }

    @Override
    public void run() {
      sessionCompletion.run();
    }
  }
}
