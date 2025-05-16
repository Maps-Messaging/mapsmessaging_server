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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamEntry;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamListResponse;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;

import java.io.IOException;
import java.util.List;

public class StreamListHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "STREAM.LIST";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    NatsFrame response = buildResponse(subject, frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }
    ((PayloadFrame) response).setPayload(buildInfo(json).getBytes());
    return response;
  }

  private String buildInfo(JsonObject json) {
    List<StreamEntry> entries = NamespaceManager.getInstance().getStreamEntries();

    // Extract pagination values
    int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
    int limit = json.has("limit") ? json.get("limit").getAsInt() : 1024;

    // Clamp values to avoid index errors
    offset = Math.max(0, Math.min(offset, entries.size()));
    int toIndex = Math.min(offset + limit, entries.size());

    List<StreamEntry> page = entries.subList(offset, toIndex);

    // Build response
    StreamListResponse streamListResponse = new StreamListResponse();
    streamListResponse.setStreams(page);
    streamListResponse.setOffset(offset);
    streamListResponse.setLimit(limit);
    streamListResponse.setTotal(entries.size());
    return gson.toJson(streamListResponse);
  }
}
