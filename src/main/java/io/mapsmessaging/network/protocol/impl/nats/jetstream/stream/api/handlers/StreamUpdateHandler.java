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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.NatsJetStreamDestinations;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamArchiveManager;
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

  private static final String TYPE = "io.nats.jetstream.api.v1.stream_update_response";

  private static final String SUBJECTS_KEY = "subjects";


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
    if (json == null || !json.has(SUBJECTS_KEY) || !json.get(SUBJECTS_KEY).isJsonArray()) {
      return new ErrFrame("Missing or invalid 'subjects' array");
    }
    PayloadFrame result = (PayloadFrame) msg;
    StreamInfoList info = NamespaceManager.getInstance().getStream(streamName);
    if (info == null) {
      result.setPayload(streamNotFound(TYPE).getBytes());
      return result;
    }
    JsonArray subjectsArray = json.getAsJsonArray(SUBJECTS_KEY);

    Set<String> newSubjects = new HashSet<>();
    subjectsArray.forEach(e -> newSubjects.add(e.getAsString()));

    Set<String> currentSubjects = info.getSubjects().stream()
        .map(s -> s.getSubject().replace('/', '.'))
        .collect(Collectors.toSet());

// Subjects to add
    Set<String> toAdd = new HashSet<>(newSubjects);
    toAdd.removeAll(currentSubjects);

// Subjects to remove
    Set<String> toRemove = new HashSet<>(currentSubjects);
    toRemove.removeAll(newSubjects);
    List<String> removedTopics = info.getSubjects().stream()
        .filter(existing -> toRemove.contains(existing.getSubject().replace('/', '.')))
        .map(existing -> existing.getDestination().getFullyQualifiedNamespace())
        .toList();

    if (!toRemove.isEmpty() && !sessionState.getProtocol().getNatsConfig().isEnableStreamDelete()) {
      return buildError(TYPE, 403,
          "Stream subject removal is not permitted", frame.getReplyTo(), sessionState);
    }
    List<CompletableFuture<Destination>> futures = new ArrayList<>();

    toAdd.stream()
        .map(subjectName -> constructDestinationFromSubject(streamName, subjectName, sessionState))
        .forEachOrdered(futures::add);

    NatsFrame failure = updateDestinations(frame, sessionState, streamName, removedTopics, futures);
    if (failure != null) {
      return failure;
    }
    PayloadFrame payloadFrame = (PayloadFrame) msg;
    JsonObject responseJson = new JsonObject();
    responseJson.addProperty("type", TYPE);
    responseJson.add("config", json.deepCopy());
    responseJson.addProperty("created", Instant.now().toString());
    payloadFrame.setPayload(gson.toJson(responseJson).getBytes(StandardCharsets.UTF_8));
    return msg;
  }

  private NatsFrame updateDestinations(PayloadFrame frame, SessionState sessionState, String streamName,
                                       List<String> removedTopics, List<CompletableFuture<Destination>> futures) {
    List<Destination> removed = new ArrayList<>();
    try {
      for (String topic : removedTopics) {
        Destination destination = sessionState.getSession().findDestination(topic, DestinationType.TOPIC)
            .get(20, TimeUnit.SECONDS);
        NatsJetStreamDestinations.checkRemoval(destination);
        removed.add(destination);
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return buildError(TYPE, 500,
          "Interrupted resolving removed subjects", frame.getReplyTo(), sessionState);
    } catch (ExecutionException | TimeoutException | IOException failure) {
      return buildError(TYPE, 500,
          "Unable to remove stream subjects: " + failure.getMessage(), frame.getReplyTo(), sessionState);
    }
    NatsFrame failure = awaitDestinations(frame, sessionState, futures);
    if (failure != null) {
      return failure;
    }
    failure = enableDestinations(frame, sessionState, streamName, futures);
    if (failure != null) {
      return failure;
    }
    return removeDestinations(frame, sessionState, streamName, removedTopics, removed);
  }

  private NatsFrame awaitDestinations(PayloadFrame frame, SessionState sessionState,
                                      List<CompletableFuture<Destination>> futures) {
    try {
      List<CompletableFuture<?>> futureList = new ArrayList<>();
      futureList.addAll(futures);
      CompletableFuture<?> all = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
      all.get(20, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      return new ErrFrame("Timed out waiting for destination creation");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return buildError(TYPE, 500,
          "Interrupted while updating stream destinations", frame.getReplyTo(), sessionState);
    } catch (ExecutionException e) {
      return buildError(TYPE, 500,
          "Failed to update stream destinations", frame.getReplyTo(), sessionState);
    }

    return null;
  }

  private NatsFrame enableDestinations(PayloadFrame frame, SessionState sessionState, String streamName,
                                       List<CompletableFuture<Destination>> futures) {
    for (CompletableFuture<Destination> pending : futures) {
      Destination destination = pending.getNow(null);
      if (destination == null) {
        return buildError(TYPE, 500, "Unable to create stream subject",
            frame.getReplyTo(), sessionState);
      }
      try {
        JetStreamArchiveManager.getInstance().ensureArchive(sessionState, streamName,
            destination.getFullyQualifiedNamespace());
        NatsJetStreamDestinations.enable(destination);
      } catch (IOException failure) {
        return buildError(TYPE, 500,
            "Unable to initialise JetStream archive: " + failure.getMessage(),
            frame.getReplyTo(), sessionState);
      }
    }
    return null;
  }

  private NatsFrame removeDestinations(PayloadFrame frame, SessionState sessionState, String streamName,
                                       List<String> removedTopics, List<Destination> removed) {
    try {
      for (Destination destination : removed) {
        JetStreamArchiveManager.getInstance().ensureArchive(sessionState, streamName,
            destination.getFullyQualifiedNamespace());
        NatsJetStreamDestinations.disable(destination);
      }
      JetStreamArchiveManager.getInstance().releaseTopics(streamName, removedTopics);
    } catch (IOException failure) {
      return buildError(TYPE, 500,
          "Unable to release stream subjects: " + failure.getMessage(), frame.getReplyTo(), sessionState);
    }
    return null;
  }

  private CompletableFuture<Destination> constructDestinationFromSubject(String streamName, String subject, SessionState sessionState) {
    String destinationName = (streamName + "." + subject).replace('.', '/');
    return sessionState.getSession().findDestination(destinationName, DestinationType.TOPIC);
  }
}
