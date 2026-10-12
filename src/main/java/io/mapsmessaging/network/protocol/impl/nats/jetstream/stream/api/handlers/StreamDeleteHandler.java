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
import java.util.ArrayList;
import java.util.List;
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

    // Resolve through the authenticated session and preflight every permission.
    // A JetStream stream is logical membership, not ownership of MAPS data.
    List<Destination> destinations = new ArrayList<>();
    try {
      for (var streamInfo : info.getSubjects()) {
        var destination = sessionState.getSession().findDestination(
            streamInfo.getDestination().getFullyQualifiedNamespace(),
            DestinationType.TOPIC).get(20, TimeUnit.SECONDS);
        NatsJetStreamDestinations.checkRemoval(destination);
        destinations.add(destination);
      }
      for (var destination : destinations) {
        // Also reconnect the persistent archive after a server restart so removal
        // releases the restored MAPS subscription rather than just the local cache.
        JetStreamArchiveManager.getInstance().ensureArchive(sessionState, streamName,
            destination.getFullyQualifiedNamespace());
      }
      for (var destination : destinations) {
        NatsJetStreamDestinations.disable(destination);
      }
      JetStreamArchiveManager.getInstance().releaseStream(streamName);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return buildError("io.nats.jetstream.api.v1.stream_delete_response", 500,
          "Interrupted removing stream membership", frame.getReplyTo(), sessionState);
    } catch (ExecutionException | TimeoutException | IOException failure) {
      return buildError("io.nats.jetstream.api.v1.stream_delete_response", 500,
          "Unable to remove stream membership: " + failure.getMessage(), frame.getReplyTo(), sessionState);
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
