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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class DurableCreateHandler extends JetStreamFrameHandler {

  private static final String TYPE = "io.nats.jetstream.api.v1.consumer_create_response";
  private final CreateHandler delegate;

  public DurableCreateHandler() {
    this(new CreateHandler());
  }

  DurableCreateHandler(CreateHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getName() {
    return "CONSUMER.DURABLE.CREATE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String originalSubject = frame.getSubject();
    String[] parts = originalSubject.split("\\.");
    if (parts.length != 7 || parts[5].isBlank() || parts[6].isBlank()) {
      return buildError(TYPE, 400, "Invalid durable consumer subject", frame.getReplyTo(), sessionState);
    }
    JsonObject request = json == null ? new JsonObject() : json.deepCopy();
    JsonObject config = request.has("config") && request.get("config").isJsonObject()
        ? request.getAsJsonObject("config") : request;
    if (config.has("durable_name") && !parts[6].equals(config.get("durable_name").getAsString())) {
      return buildError(TYPE, 400, "Durable name does not match request subject", frame.getReplyTo(), sessionState);
    }
    config.addProperty("durable_name", parts[6]);
    config.addProperty("name", parts[6]);

    frame.setSubject("$JS.API.CONSUMER.CREATE." + parts[5] + "." + parts[6]);
    try {
      return delegate.handle(frame, request, sessionState);
    } finally {
      frame.setSubject(originalSubject);
    }
  }
}
