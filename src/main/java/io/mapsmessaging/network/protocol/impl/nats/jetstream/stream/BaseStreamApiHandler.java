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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.time.Instant;

public class BaseStreamApiHandler {

  protected final Gson gson;

  public BaseStreamApiHandler() {
    gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .create();
  }

  protected NatsFrame buildResponse(String subject, PayloadFrame payloadFrame, SessionState sessionState) {
    String replyTo = payloadFrame.getReplyTo();
    if (replyTo == null || replyTo.isEmpty()) {
      return new ErrFrame("Missing reply subject");
    }

    MsgFrame msg = new MsgFrame(0);
    msg.setSubject(replyTo);
    msg.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    return msg;
  }
}
