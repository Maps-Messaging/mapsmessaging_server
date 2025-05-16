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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public abstract class RequestHandler extends BaseStreamApiHandler {

  protected final JetStreamFrameHandler[] handlers;

  protected RequestHandler(JetStreamFrameHandler[] handlers){
    this.handlers = handlers;
  }

  public NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException {
    String action = subject.substring("$JS.API.".length());
    byte[] data = frame.getPayload();
    JsonObject json = (data != null && data.length > 0) ? JsonParser.parseString(new String(data)).getAsJsonObject() : null;
    for (JetStreamFrameHandler handler : handlers) {
      if (action.startsWith(handler.getName())) {
        return handler.handle(frame, json, sessionState);
      }
    }
    return createError(sessionState.getJetStreamRequestManager().getJetSubject(),
        sessionState.getJetStreamRequestManager().getSid(frame.getReplyTo()),
        "Function not implemented: " + subject);
  }


  public abstract String getType();



  protected NatsFrame createError(String subject, String subscriptionId, String errorMsg) {
    MsgFrame errorFrame = new MsgFrame(0);
    errorFrame.setSubject(subject);
    errorFrame.setSubscriptionId(subscriptionId);
    JsonObject root = new JsonObject();
    root.addProperty("type", "io.nats.jetstream.api.v1.error");

    JsonObject error = new JsonObject();
    error.addProperty("code", 501);
    error.addProperty("err_code", 1);
    error.addProperty("description", errorMsg);

    root.add("error", error);
    errorFrame.setPayload(gson.toJson(root).getBytes());
    return errorFrame;
  }

}
