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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.info.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamStorageUsage;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InfoHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "INFO";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    NatsFrame response = buildResponse(frame.getSubject(), frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }
    ((PayloadFrame) response).setPayload(buildInfo(sessionState).getBytes(StandardCharsets.UTF_8));
    return response;
  }

  private String buildInfo(SessionState sessionState) {
    NamespaceManager namespaces = NamespaceManager.getInstance();
    long memoryBytes = 0;
    long storageBytes = 0;
    for (var stream : namespaces.getStreamEntries()) {
      StreamInfoList streamInfo = namespaces.getStream(stream.getConfig().getName());
      if (streamInfo == null) {
        continue;
      }
      for (StreamInfo info : streamInfo.getSubjects()) {
        DestinationImpl destination = info.getDestination();
        long bytes = JetStreamStorageUsage.sampledPhysicalBytes(destination);
        if (destination.isPersistent()) {
          storageBytes += bytes;
        } else {
          memoryBytes += bytes;
        }
      }
    }

    JsonObject limits = new JsonObject();
    limits.addProperty("max_memory", -1);
    limits.addProperty("max_storage", -1);
    limits.addProperty("max_streams", -1);
    limits.addProperty("max_consumers", -1);
    limits.addProperty("max_ack_pending", -1);
    limits.addProperty("memory_max_stream_bytes", -1);
    limits.addProperty("storage_max_stream_bytes", -1);
    limits.addProperty("max_bytes_required", false);

    JsonObject api = new JsonObject();
    api.addProperty("total", sessionState.getJetStreamRequestManager().getApiRequests());
    api.addProperty("errors", sessionState.getJetStreamRequestManager().getApiErrors());
    api.addProperty("level", 0);

    JsonObject response = new JsonObject();
    response.addProperty("type", "io.nats.jetstream.api.v1.account_info_response");
    response.addProperty("memory", memoryBytes);
    response.addProperty("storage", storageBytes);
    response.addProperty("reserved_memory", 0);
    response.addProperty("reserved_storage", 0);
    response.addProperty("streams", namespaces.getStreamEntries().size());
    response.addProperty("consumers", sessionState.getNamedConsumers().size());
    response.add("limits", limits);
    response.add("api", api);
    return gson.toJson(response);
  }
}
