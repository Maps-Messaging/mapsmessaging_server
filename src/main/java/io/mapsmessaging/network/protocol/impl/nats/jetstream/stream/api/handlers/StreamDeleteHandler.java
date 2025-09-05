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
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StreamDeleteHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "STREAM.DELETE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    NatsFrame msg = buildResponse(subject, frame, sessionState);
    if (msg instanceof ErrFrame) {
      return msg;
    }

    String[] parts = subject.split("\\.");
    if (parts.length < 5) {
      return new ErrFrame("Invalid stream delete subject");
    }
    PayloadFrame result = (PayloadFrame) msg;
    if (!sessionState.getProtocol().getNatsConfig().isEnableStreamDelete()) {
      result.setPayload(deletionDisabledMessage().getBytes());
      return result;
    }

    String streamName = parts[4];
    StreamInfoList info = NamespaceManager.getInstance().getStream(streamName);
    if (info == null) {
      result.setPayload(streamNotFound("io.nats.jetstream.api.v1.stream_delete_response").getBytes());
      return result;
    }

    List<CompletableFuture<Void>> deletes = new ArrayList<>();

    for (StreamInfo streamInfo : info.getSubjects()) {
      deletes.add(sessionState.getSession().deleteDestinationImpl(streamInfo.getDestination()));
    }

    try {
      CompletableFuture<Void> all = CompletableFuture.allOf(deletes.toArray(new CompletableFuture[0]));
      all.get(20, TimeUnit.SECONDS); // Optional timeout
    } catch (TimeoutException e) {
      return new ErrFrame("Timed out while deleting stream destinations");
    } catch (ExecutionException | InterruptedException e) {
      return new ErrFrame("Error while deleting stream destinations");
    }
    result.setPayload(success().getBytes());

    return result;
  }

  private String success() {
    return "{\n" +
        "  \"type\": \"io.nats.jetstream.api.v1.stream_delete_response\",\n" +
        "  \"success\": true\n" +
        "}";
  }

  private String deletionDisabledMessage() {
    return "{\n" +
        "  \"type\": \"io.nats.jetstream.api.v1.stream_delete_response\",\n" +
        "  \"error\": {\n" +
        "    \"code\": 403,\n" +
        "    \"description\": \"stream deletion not permitted\"\n" +
        "  }\n" +
        "}\n";
  }
}
