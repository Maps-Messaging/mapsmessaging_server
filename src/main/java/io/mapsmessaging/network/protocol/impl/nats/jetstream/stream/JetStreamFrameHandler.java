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
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class JetStreamFrameHandler extends BaseStreamApiHandler {
  public abstract String getName();

  public abstract NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException;

  protected String streamNotFound(String type) {
    return "{\n" +
        "  \"type\": \"" + type + "\",\n" +
        "  \"error\": {\n" +
        "    \"code\": 404,\n" +
        "    \"description\": \"stream not found\"\n" +
        "  }\n" +
        "}";
  }


  protected NatsFrame buildError(String type, String message, String replyTo, SessionState sessionState) {
    JsonObject err = new JsonObject();
    err.addProperty("code", 404);
    err.addProperty("description", message);

    JsonObject root = new JsonObject();
    root.addProperty("type", type);
    root.add("error", err);

    PayloadFrame frame = new MsgFrame(0);
    frame.setSubject(replyTo);
    frame.setSubscriptionId(sessionState.getJetStreamRequestManager().getSid(replyTo));
    frame.setPayload(gson.toJson(root).getBytes(StandardCharsets.UTF_8));
    return frame;
  }

}
