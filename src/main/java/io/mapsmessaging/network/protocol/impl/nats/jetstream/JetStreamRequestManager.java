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

package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamApiManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.charset.StandardCharsets;

public class JetStreamRequestManager {
  private final JetStreamApiManager jetStreamApiManager;
  private final AtomicLong apiRequests = new AtomicLong();
  private final AtomicLong apiErrors = new AtomicLong();

  private final Map<String, String> subscriptionId = new ConcurrentHashMap<>();

  @Getter
  @Setter
  private String jetSubject;

  public JetStreamRequestManager() {
    jetStreamApiManager = new JetStreamApiManager();
  }

  public void close(){
    subscriptionId.clear();
  }

  public void registerSid(String subscriptionPattern, String sid) {
    if (subscriptionPattern != null && sid != null) {
      subscriptionId.put(subscriptionPattern, sid);
    }
  }

  public void unregisterSid(String sid) {
    subscriptionId.entrySet().removeIf(entry -> entry.getValue().equals(sid));
  }

  public String getSid(String reply) {
    if (reply == null || reply.isEmpty()) {
      return "";
    }
    String mostSpecific = "";
    String sid = "";
    for (Map.Entry<String, String> entry : subscriptionId.entrySet()) {
      if (entry.getKey().length() > mostSpecific.length() && matchesInbox(entry.getKey(), reply)) {
        mostSpecific = entry.getKey();
        sid = entry.getValue();
      }
    }
    return sid;
  }

  private static boolean matchesInbox(String subscription, String reply) {
    if (subscription.equals(reply)) {
      return true;
    }
    int wildcard = subscription.lastIndexOf('.');
    if (wildcard <= 0) {
      return false;
    }
    String token = subscription.substring(wildcard + 1);
    String prefix = subscription.substring(0, wildcard + 1);
    if (!reply.startsWith(prefix)) {
      return false;
    }
    String remaining = reply.substring(prefix.length());
    if (remaining.isEmpty()) {
      return false;
    }
    return ">".equals(token) || ("*".equals(token) && !remaining.contains("."));
  }

  public boolean isJetStreamRequest(PayloadFrame frame) {
    String subject = frame.getSubject();
    return (subject != null && (
        subject.startsWith("$JS.")
            || subject.startsWith("$KV.")
            || subject.startsWith("$O.")));
  }

  public long getApiRequests() {
    return apiRequests.get();
  }

  public long getApiErrors() {
    return apiErrors.get();
  }

  public NatsFrame process(PayloadFrame frame, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    boolean isApi = subject.startsWith("$JS.API.");
    if (isApi) {
      apiRequests.incrementAndGet();
    }

    NatsFrame response;
    if (subject.startsWith("$JS.")) {
      if (sessionState.getProtocol().getNatsConfig().isEnableStreams()) {
        response = jetStreamApiManager.process(subject, frame, sessionState);
      } else {
        response = new ErrFrame("Streams are disabled");
      }
    } else if (subject.startsWith("$KV.")) {
      response = new ErrFrame(sessionState.getProtocol().getNatsConfig().isEnableKeyValues()
          ? "KeyValue handler not yet implemented" : "Key Values are disabled");
    } else if (subject.startsWith("$O.")) {
      response = new ErrFrame(sessionState.getProtocol().getNatsConfig().isEnableObjectStore()
          ? "ObjectStore handler not yet implemented" : "Object Store is disabled");
    } else {
      response = new ErrFrame("Unknown JetStream request: " + subject);
    }

    if (isApi && isError(response)) {
      apiErrors.incrementAndGet();
    }
    return response;
  }

  private boolean isError(NatsFrame frame) {
    if (frame instanceof ErrFrame) {
      return true;
    }
    if (frame instanceof PayloadFrame payload && payload.getPayload() != null) {
      try {
        return JsonParser.parseString(new String(payload.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject().has("error");
      } catch (RuntimeException ignored) {
        return false;
      }
    }
    return false;
  }
}
