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
    if (!validateStreamSequences(engine, publisherReply, lookup, destinations)) {
      return;
    }
    Publication publication = new Publication();
    for (String namespace : destinations) {
      if (!publishDestination(engine, msgFrame, lookup, namespace, publisherReply, publication)) {
        return;
      }
    }
    sendPublicationResult(engine, publisherReply, publication);
    if (engine.isVerbose()) {
      engine.send(new OkFrame());
    }
  }

  private boolean validateStreamSequences(SessionState engine, String publisherReply, String lookup,
                                          Set<String> destinations) {
    if (publisherReply != null && !publisherReply.isBlank()) {
      for (String namespace : destinations) {
        if (!namespace.equals(lookup) && !supportsDestinationSequence(namespace)) {
          sendPublishError(engine, publisherReply,
              "JetStream stream-global sequences across multiple MAPS destinations are not implemented", 501);
          return false;
        }
      }
    }
    return true;
  }

  private boolean publishDestination(SessionState engine, PayloadFrame msgFrame, String lookup, String namespace,
                                     String publisherReply, Publication publication) throws IOException {
    CompletableFuture<Destination> future = engine.getSession().findDestination(namespace, DestinationType.TOPIC);
    if (future == null) {
      return false;
    }
    try {
      Destination destination = future.get();
      if (destination == null) {
        engine.send(new ErrFrame("No such destination"));
        return false;
      }
      boolean streamTarget = !namespace.equals(lookup);
      if (streamTarget) {
        initialiseStreamArchive(engine, namespace, destination);
      }
      long assignedId = handleMessageStoreToDestination(destination, engine, msgFrame, streamTarget);
      if (streamTarget) {
        publication.record(namespace, assignedId);
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
      sendDestinationError(engine, publisherReply, namespace.equals(lookup), e);
      return false;
    }
    return true;
  }

  private void initialiseStreamArchive(SessionState engine, String namespace, Destination destination) throws IOException {
    int separator = namespace.indexOf('/');
    if (separator <= 0) {
      throw new IOException("Invalid JetStream destination: " + namespace);
    }
    ensureArchiveForStream(engine, namespace.substring(0, separator), destination.getFullyQualifiedNamespace());
  }

  private void sendDestinationError(SessionState engine, String publisherReply, boolean coreTarget, IOException failure) {
    if (publisherReply != null && !publisherReply.isBlank() && !coreTarget) {
      sendPublishError(engine, publisherReply, failure.getMessage());
    } else {
      engine.send(new ErrFrame(failure.getMessage()));
    }
  }

  private void sendPublicationResult(SessionState engine, String publisherReply, Publication publication) {
    if (publisherReply != null && !publisherReply.isBlank() && publication.streamPublication) {
      if (publication.sequence > 0) {
        sendPublishAcknowledgement(engine, publisherReply, publication.stream, publication.sequence);
      } else {
        sendPublishError(engine, publisherReply, "JetStream message was not stored");
      }
    }
  }

  private static class Publication {
    private String stream;
    private long sequence;
    private boolean streamPublication;

    private void record(String namespace, long assignedId) {
      streamPublication = true;
      if (stream == null) {
        stream = namespace.substring(0, namespace.indexOf('/'));
        sequence = assignedId;
      }
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
    sendPublishError(engine, replyTo, description, 500);
  }

  private void sendPublishError(SessionState engine, String replyTo, String description, int code) {
    JsonObject error = new JsonObject();
    error.addProperty("code", code);
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

  protected boolean supportsDestinationSequence(String namespace) {
    return NamespaceManager.getInstance().supportsDestinationSequence(namespace);
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
