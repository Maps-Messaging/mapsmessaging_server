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


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class StreamUpdateHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "STREAM.UPDATE";
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
      return new ErrFrame("Invalid stream create subject");
    }
    String streamName = parts[4];

    // Process subjects
    if (!json.has("subjects") || !json.get("subjects").isJsonArray()) {
      return new ErrFrame("Missing or invalid 'subjects' array");
    }
    PayloadFrame result = (PayloadFrame) msg;
    StreamInfoList info = NamespaceManager.getInstance().getStream(streamName);
    if (info == null) {
      result.setPayload(streamNotFound("io.nats.jetstream.api.v1.stream_delete_response").getBytes());
      return result;
    }
    JsonArray subjectsArray = json.getAsJsonArray("subjects");

    Set<String> newSubjects = new HashSet<>();
    subjectsArray.forEach(e -> newSubjects.add(e.getAsString()));

    Set<String> currentSubjects = info.getSubjects().stream()
        .map(s -> s.getDestination().getFullyQualifiedNamespace())
        .collect(Collectors.toSet());

// Subjects to add
    Set<String> toAdd = new HashSet<>(newSubjects);
    toAdd.removeAll(currentSubjects);

// Subjects to remove
    Set<String> toRemove = new HashSet<>(currentSubjects);
    toRemove.removeAll(newSubjects);

    List<CompletableFuture<Destination>> futures = new ArrayList<>();

    for (String subjectName : toAdd) {
      futures.add(constructDestinationFromSubject(streamName, subjectName, sessionState));
    }

    List<CompletableFuture<Void>> deletes = new ArrayList<>();
    if (sessionState.getProtocol().getNatsConfig().isEnableStreamDelete()) {
      for (String subjectName : toRemove) {
        info.getSubjects().stream()
            .filter(s -> s.getDestination().getFullyQualifiedNamespace().equals(subjectName))
            .findFirst()
            .ifPresent(s -> deletes.add(sessionState.getSession().deleteDestinationImpl(s.getDestination())));
      }
    }
    try {
      List<CompletableFuture<?>> futureList = new ArrayList<>();
      futureList.addAll(deletes);
      futureList.addAll(futures);
      CompletableFuture<?> all = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
      all.get(20, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      return new ErrFrame("Timed out waiting for destination creation");
    } catch (ExecutionException | InterruptedException e) {
      return new ErrFrame("Failed to create one or more destinations");
    }

    PayloadFrame payloadFrame = (PayloadFrame) msg;
    JsonObject responseJson = new JsonObject();
    responseJson.addProperty("type", "io.nats.jetstream.api.v1.stream_create_response");
    responseJson.add("config", json.deepCopy());
    responseJson.addProperty("created", Instant.now().toString());
    payloadFrame.setPayload(gson.toJson(responseJson).getBytes(StandardCharsets.UTF_8));
    return msg;
  }

  private CompletableFuture<Destination> constructDestinationFromSubject(String streamName, String subject, SessionState sessionState) {
    String destinationName = (streamName + "." + subject).replace('.', '/');
    return sessionState.getSession().findDestination(destinationName, DestinationType.TOPIC);
  }
}
