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
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.nats.frames.*;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;

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
        handleMessageStoreToDestination(destination, engine, msgFrame);
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
        engine.send(new ErrFrame(e.getMessage()));
        return;
      }
    }
    if (engine.isVerbose()) {
      engine.send(new OkFrame());
    }
  }

  protected List<String> getMatchingStreamDestinations(String subject) {
    return NamespaceManager.getInstance().getMatchingStreamDestinations(subject);
  }

  protected void handleMessageStoreToDestination(Destination destination, SessionState engine, PayloadFrame msgFrame) throws IOException {
    if (destination != null) {
      Map<String, TypedData> dataMap = new HashMap<>();
      Map<String, String> metaData = new HashMap<>();
      metaData.put("protocol", "NATS");
      metaData.put("version", engine.getProtocol().getVersion());
      metaData.put("sessionId", engine.getSession().getName());
      metaData.put("time_ms", "" + System.currentTimeMillis());

      MessageBuilder mb = new MessageBuilder();
      mb.setDataMap(dataMap)
          .setOpaqueData(msgFrame.getPayload())
          .setMeta(metaData)
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
      destination.storeMessage(message);
    }
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
