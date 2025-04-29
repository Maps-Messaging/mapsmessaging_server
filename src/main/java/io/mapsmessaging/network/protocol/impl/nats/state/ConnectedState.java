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

import java.io.ByteArrayOutputStream;
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
    NatsFrame msg;
    if(engine.isHeaders() && !message.getDataMap().isEmpty()){
      msg = buildHMsgFrame(engine, destinationName, context, message);
    }
    else{
      msg = buildMsgFrame(engine, destinationName, context, message);
    }
    return engine.send(msg);
  }

  private NatsFrame buildMsgFrame(SessionState engine, String destinationName, SubscriptionContext context, Message message) {
    byte[] payloadData = message.getOpaqueData();
    MsgFrame msgFrame = new MsgFrame(engine.getMaxBufferSize());
    msgFrame.setSubject(mapMqttTopicToNatsSubject(destinationName));
    msgFrame.setSubscriptionId(context.getAlias());
    if(message.getCorrelationData() != null){
      msgFrame.setReplyTo(new String(message.getCorrelationData()));
    }
    msgFrame.setPayloadSize(payloadData.length);
    msgFrame.setPayload(payloadData);
    return msgFrame;
  }

  private NatsFrame buildHMsgFrame(SessionState engine, String destinationName, SubscriptionContext context, Message message) {
    byte[] payloadData = message.getOpaqueData();
    HMsgFrame msgFrame = new HMsgFrame(engine.getMaxBufferSize());
    msgFrame.setSubject(mapMqttTopicToNatsSubject(destinationName));
    msgFrame.setSubscriptionId(context.getAlias());
    if(message.getCorrelationData() != null){
      msgFrame.setReplyTo(new String(message.getCorrelationData()));
    }
    StringBuilder sb = new StringBuilder("NATS/1.0\r\n");
    for(Map.Entry<String, TypedData> entry: message.getDataMap().entrySet()){
      sb.append(entry.getKey().replace(" ", "_")).append(": ").append(""+entry.getValue().getData()).append("\r\n");
    }
    sb.append("\r\n");
    byte[] header = sb.toString().getBytes();
    msgFrame.setHeaderBytes(header);
    msgFrame.setPayloadSize(payloadData.length);
    msgFrame.setPayload(payloadData);
    return msgFrame;
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
