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

package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.NatsJetStreamStorage;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.nats.frames.*;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamArchiveManager;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PubListener implements FrameListener {

  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {
    PayloadFrame msgFrame = (PayloadFrame) frame;
    if (engine.getJetStreamRequestManager().isJetStreamRequest(msgFrame)) {
      NatsFrame response = engine.getJetStreamRequestManager().process(msgFrame, engine);
      if (response != null) {
        engine.send(response);
      }
      return;
    }
    String destName = convertSubject(msgFrame.getSubject());
    String lookup = engine.getMapping(destName);
    Set<String> destinations = new LinkedHashSet<>();
    destinations.add(lookup);
    if (engine.getProtocol().getNatsConfig().isEnableStreams()) {
      // Resolve stream targets before finding the public destination, since finding it can create a MAPS namespace.
      destinations.addAll(getMatchingStreamDestinations(msgFrame.getSubject()));
    }

    String publisherReply = msgFrame.getReplyTo();
    String publishedStream = null;
    long publishedSequence = 0;
    boolean streamPublication = false;
    for (String namespace : destinations) {
      CompletableFuture<Destination> future = engine.getSession().findDestination(namespace, DestinationType.TOPIC);
      if (future == null) {
        return;
      }
      try {
        Destination destination = future.get();
        if (destination == null) {
          engine.send(new ErrFrame("No such destination"));
          return;
        }
        boolean streamTarget = !namespace.equals(lookup);
        if (streamTarget) {
          int separator = namespace.indexOf('/');
          if (separator <= 0) {
            throw new IOException("Invalid JetStream destination: " + namespace);
          }
          ensureArchiveForStream(engine, namespace.substring(0, separator),
              destination.getFullyQualifiedNamespace());
        }
        long assignedId = handleMessageStoreToDestination(destination, engine, msgFrame, streamTarget);
        if (streamTarget) {
          streamPublication = true;
          if (publishedStream == null) {
            publishedStream = namespace.substring(0, namespace.indexOf('/'));
            publishedSequence = assignedId;
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while resolving NATS destination", e);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException ioException) {
          throw ioException;
        }
        throw new IOException("Failed to resolve NATS destination", cause);
      } catch (IOException e) {
        if (publisherReply != null && !publisherReply.isBlank() && !namespace.equals(lookup)) {
          sendPublishError(engine, publisherReply, e.getMessage());
        } else {
          engine.send(new ErrFrame(e.getMessage()));
        }
        return;
      }
    }
    if (publisherReply != null && !publisherReply.isBlank() && streamPublication) {
      if (publishedSequence > 0) {
        sendPublishAcknowledgement(engine, publisherReply, publishedStream, publishedSequence);
      } else {
        sendPublishError(engine, publisherReply, "JetStream message was not stored");
      }
    }
    if (engine.isVerbose()) {
      engine.send(new OkFrame());
    }
  }

  private void sendPublishAcknowledgement(SessionState engine, String replyTo, String stream, long sequence) {
    JsonObject body = new JsonObject();
    body.addProperty("stream", stream);
    body.addProperty("seq", sequence);
    body.addProperty("duplicate", false);
    sendPublishResponse(engine, replyTo, body);
  }

  private void sendPublishError(SessionState engine, String replyTo, String description) {
    JsonObject error = new JsonObject();
    error.addProperty("code", 500);
    error.addProperty("description", description);
    JsonObject body = new JsonObject();
    body.add("error", error);
    sendPublishResponse(engine, replyTo, body);
  }

  private void sendPublishResponse(SessionState engine, String replyTo, JsonObject body) {
    MsgFrame response = new MsgFrame(0);
    response.setSubject(replyTo);
    response.setSubscriptionId(engine.getJetStreamRequestManager().getSid(replyTo));
    response.setPayload(body.toString().getBytes(StandardCharsets.UTF_8));
    engine.send(response);
  }

  protected void ensureArchiveForStream(SessionState connection, String stream, String destination) throws IOException {
    JetStreamArchiveManager.getInstance().ensureArchive(connection, stream, destination);
  }

  protected List<String> getMatchingStreamDestinations(String subject) {
    return NamespaceManager.getInstance().getMatchingStreamDestinations(subject);
  }

  protected long handleMessageStoreToDestination(Destination destination, SessionState engine, PayloadFrame msgFrame, boolean streamTarget) throws IOException {
    if (destination != null) {
      Map<String, TypedData> dataMap = new HashMap<>();
      Map<String, String> metaData = new HashMap<>();
      metaData.put("protocol", "NATS");
      metaData.put("version", engine.getProtocol().getVersion());
      metaData.put("sessionId", engine.getSession().getName());
      metaData.put("time_ms", "" + System.currentTimeMillis());
      if (streamTarget) {
        metaData.put("nats.jetstream", "true");
        metaData.put("nats.subject", msgFrame.getSubject());
      }

      MessageBuilder mb = new MessageBuilder();
      mb.setDataMap(dataMap)
          .setOpaqueData(msgFrame.getPayload())
          .setMeta(metaData)
          .storeOffline(streamTarget)
          .setCorrelationData(msgFrame.getReplyTo())
          .setTransformation(engine.getProtocol().getProtocolMessageTransformation());
      Message message = MessageOverrides.createMessageBuilder(engine.getProtocol().getProtocolConfig().getMessageDefaults(), mb).build();
      if (msgFrame instanceof HPayloadFrame matchedHPayloadFrame) {
        Map<String, String> headers = (matchedHPayloadFrame).getHeader();
        Map<String, TypedData> map = message.getDataMap();
        if (headers != null) {
          for (Map.Entry<String, String> entry : headers.entrySet()) {
            map.put(entry.getKey(), new TypedData(convert(entry.getValue())));
          }
        }
      }
      return storeToMapDestination(destination, message, streamTarget);
    }
    return 0;
  }

  protected long storeToMapDestination(Destination destination, Message message, boolean streamTarget) throws IOException {
    if (streamTarget) {
      return NatsJetStreamStorage.store(destination, message);
    }
    destination.storeMessage(message);
    return 0;
  }

  private Object convert(String value) {
    Number n = tryParseNumber(value);
    return n != null ? n : value;
  }

  private Number tryParseNumber(String value) {
    String trimmed = value.trim();
    try {
      return Long.parseLong(trimmed);
    } catch (NumberFormatException e1) {
      try {
        return Double.parseDouble(trimmed);
      } catch (NumberFormatException e2) {
        return null;
      }
    }
  }
}
