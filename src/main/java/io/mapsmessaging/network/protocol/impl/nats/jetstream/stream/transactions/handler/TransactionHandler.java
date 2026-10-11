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
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;

import java.io.IOException;

public class TransactionHandler extends JetStreamFrameHandler {

  private final TransactionProcessor ackProcessor = new AckProcessor();
  private final TransactionProcessor nackProcessor = new NackProcessor();

  @Override
  public String getName() {
    return "$JS.ACK";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();

    String[] parts = subject.split("\\.");
    if (parts.length == 9) {
      TransactionSubject transactionSubject;
      try {
        transactionSubject = TransactionSubject.parse(subject);
      } catch (IllegalArgumentException exception) {
        return null;
      }
      StreamSubscriptionInfo info = lookupInfo(transactionSubject, sessionState);
      if (info != null) {
        String commandString = frame.getPayload() == null ? "" : new String(frame.getPayload(), java.nio.charset.StandardCharsets.UTF_8).trim().toLowerCase(java.util.Locale.ROOT);
        switch (commandString) {
          case "", "+ack", "+term":
            ackProcessor.handle(info, transactionSubject);
            return confirmation(frame, sessionState);
          case "-nak":
            nackProcessor.handle(info, transactionSubject);
            return confirmation(frame, sessionState);

          default:
            if (commandString.startsWith("-nak ")) {
              nackProcessor.handle(info, transactionSubject);
              return confirmation(frame, sessionState);
            }
            break;
        }
      }
    }
    return null;
  }


  private NatsFrame confirmation(PayloadFrame request, SessionState sessionState) {
    String replyTo = request.getReplyTo();
    if (replyTo == null || replyTo.isBlank()) {
      return null;
    }
    MsgFrame reply = new MsgFrame(0);
    reply.setSubject(replyTo);
    reply.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    reply.setPayload(new byte[0]);
    return reply;
  }

  private StreamSubscriptionInfo lookupInfo( TransactionSubject transactionSubject, SessionState sessionState ) {
    NamedConsumer consumer = sessionState.getNamedConsumers().get(transactionSubject.getConsumer());
    if (consumer != null && transactionSubject.getDestinationIndex() >= 0 && transactionSubject.getDestinationIndex() < consumer.getStreams().size()) {
      return consumer.getStreams().get(transactionSubject.getDestinationIndex());
    }
    return null;
  }
}
