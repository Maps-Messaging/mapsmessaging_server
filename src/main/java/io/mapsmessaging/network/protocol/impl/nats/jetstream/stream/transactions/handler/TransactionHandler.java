/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler.MessageHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class TransactionHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "$JS.ACK";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState state) throws IOException {
    TransactionSubject subject;
    try {
      subject = TransactionSubject.parse(frame.getSubject());
    } catch (IllegalArgumentException invalid) {
      return null;
    }
    NamedConsumer consumer = state.getNamedConsumers().get(subject.getConsumer());
    if (consumer == null || !consumer.getStreamName().equals(subject.getStream())) {
      return null;
    }

    String command = frame.getPayload() == null ? "" : new String(frame.getPayload(), StandardCharsets.UTF_8).trim();
    String keyword = command.toUpperCase(Locale.ROOT);
    if (keyword.isEmpty() || "+ACK".equals(keyword)) {
      if (consumer.acknowledge(subject, false) || consumer.isAcknowledged(subject)) {
        return confirmation(frame, state);
      }
      return null;
    }
    if ("+TERM".equals(keyword) || keyword.startsWith("+TERM ")) {
      if (consumer.acknowledge(subject, true) || consumer.isAcknowledged(subject)) {
        return confirmation(frame, state);
      }
      return null;
    }
    if ("+WPI".equals(keyword)) {
      return consumer.workingProgress(subject) ? confirmation(frame, state) : null;
    }
    if ("-NAK".equals(keyword) || keyword.startsWith("-NAK ")) {
      Long delay = parseNakDelay(command);
      return delay != null && consumer.negativeAcknowledgement(subject, delay) ? confirmation(frame, state) : null;
    }
    if ("+NXT".equals(keyword) || keyword.startsWith("+NXT ")) {
      NextDeliveryRequest request = parseNextRequest(command);
      if (request == null || !consumer.acknowledge(subject, false)) {
        return null;
      }
      String replyTo = frame.getReplyTo();
      if (replyTo != null && !replyTo.isBlank()) {
        new MessageHandler().deliverNext(consumer, replyTo, state, request.batch, request.noWait);
      }
      return null;
    }
    return null;
  }

  static Long parseNakDelay(String command) {
    String body = command.substring(4).trim();
    if (body.isEmpty()) {
      return 0L;
    }
    try {
      JsonObject request = JsonParser.parseString(body).getAsJsonObject();
      long delay = request.get("delay").getAsLong();
      return delay >= 0 ? delay : null;
    } catch (RuntimeException invalid) {
      return null;
    }
  }

  private static NextDeliveryRequest parseNextRequest(String command) {
    String body = command.substring(4).trim();
    if (body.isEmpty()) {
      return new NextDeliveryRequest(1, false);
    }
    try {
      int batch;
      boolean noWait = false;
      if (body.startsWith("{")) {
        JsonObject object = JsonParser.parseString(body).getAsJsonObject();
        batch = object.has("batch") ? object.get("batch").getAsInt() : 1;
        noWait = object.has("no_wait") && object.get("no_wait").getAsBoolean();
      } else {
        batch = Integer.parseInt(body);
      }
      return batch > 0 && batch <= 1024 ? new NextDeliveryRequest(batch, noWait) : null;
    } catch (RuntimeException invalid) {
      return null;
    }
  }

  private NatsFrame confirmation(PayloadFrame request, SessionState state) {
    String replyTo = request.getReplyTo();
    if (replyTo == null || replyTo.isBlank()) {
      return null;
    }
    MsgFrame reply = new MsgFrame(0);
    reply.setSubject(replyTo);
    reply.setSubscriptionId(state.getJetStreamRequestManager().getSid(replyTo));
    reply.setPayload(new byte[0]);
    return reply;
  }

  private record NextDeliveryRequest(int batch, boolean noWait) {
  }
}
