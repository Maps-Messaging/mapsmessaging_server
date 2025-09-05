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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class InfoHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "INFO";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    NatsFrame response = super.buildResponse(frame.getSubject(), frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }
    ((PayloadFrame) response).setPayload(buildInfo().getBytes());
    return response;
  }

  private String buildInfo() {
    JsonObject root = new JsonObject();
    root.addProperty("type", "io.nats.jetstream.api.v1.info_response");
    root.addProperty("server_id", MessageDaemon.getInstance().getUuid().toString());
    root.addProperty("server_name", MessageDaemon.getInstance().getHostname());
    root.addProperty("version", BuildInfo.getBuildVersion());
    root.addProperty("jetstream", true);

    JsonObject config = new JsonObject();
    config.addProperty("api_prefix", "$JS.API");
    root.add("config", config);
    return gson.toJson(root);
  }

}
