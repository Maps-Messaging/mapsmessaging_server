package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StreamCreateHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "CREATE";
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

    JsonArray subjectsArray = json.getAsJsonArray("subjects");
    List<CompletableFuture<Destination>> futures = new ArrayList<>();

    for (JsonElement entry : subjectsArray) {
      String subjectName = entry.getAsString();
      futures.add(constructDestinationFromSubject(streamName, subjectName, sessionState));
    }
    try {
      CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
      all.get(20, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      return new ErrFrame("Timed out waiting for destination creation");
    } catch (ExecutionException | InterruptedException e) {
      return new ErrFrame("Failed to create one or more destinations");
    }

    // Prepare response
    JsonObject response = new JsonObject();
    response.addProperty("type", "io.nats.jetstream.api.v1.stream_create_response");
    response.add("config", json.deepCopy());
    response.addProperty("created", Instant.now().toString());

    PayloadFrame payloadFrame = (PayloadFrame) msg;
    payloadFrame.setPayload(new Gson().toJson(response).getBytes(StandardCharsets.UTF_8));
    return msg;
  }

  private CompletableFuture<Destination> constructDestinationFromSubject(String streamName, String subject, SessionState sessionState) {
    String destinationName = (streamName + "." + subject).replace('.', '/');
    return sessionState.getSession().findDestination(destinationName, DestinationType.TOPIC);
  }
}

