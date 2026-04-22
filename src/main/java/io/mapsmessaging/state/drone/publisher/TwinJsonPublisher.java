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

package io.mapsmessaging.state.drone.publisher;

import com.google.gson.Gson;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.utilities.GsonFactory;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TwinJsonPublisher implements TwinObserver, ClientConnection, MessageListener, AutoCloseable {

  private final Session session;
  private final Gson gson;
  private final String topicTemplate;
  private final TwinManager twinManager;
  private final Map<String, Destination> destinationCache;

  public TwinJsonPublisher(TwinManager twinManager, String topicTemplate) throws ExecutionException, InterruptedException, TimeoutException {
    this.topicTemplate = topicTemplate;
    this.twinManager = twinManager;
    this.gson = GsonFactory.getInstance().getTimeSafeGson();
    this.destinationCache = new ConcurrentHashMap<>();
    this.session = createSession();
    twinManager.addObserver(this);
  }

  @Override
  public void close() throws IOException {
    twinManager.removeObserver(this);
    SessionManager.getInstance().close(session, true);
  }

  @Override
  public void onTwinUpdated(String twinId, EntityTwin previous, EntityTwin current, TwinUpdateContext context) {
    if (twinId == null || twinId.isBlank() || current == null) {
      return;
    }

    try {
      publishTwin(twinId, current);
    }
    catch (Exception exception) {
      throw new RuntimeException("Failed to publish twin update for twinId=" + twinId, exception);
    }
  }

  public void publishTwin(String twinId, EntityTwin twin) throws ExecutionException, InterruptedException, TimeoutException, IOException {
    String topic = resolveTopic(twinId, twin);
    Destination destination = resolveDestination(topic);
    try {
      String json = gson.toJson(twin);

      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8))
          .setQoS(QualityOfService.AT_MOST_ONCE)
          .setContentType("application/json")
          .setSchemaId(SchemaManager.DEFAULT_JSON_SCHEMA.toString())
          .setRetain(false);

      destination.storeMessage(messageBuilder.build());
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private Destination resolveDestination(String topic) throws ExecutionException, InterruptedException, TimeoutException {
    Destination destination = destinationCache.get(topic);
    if (destination != null) {
      return destination;
    }

    Destination resolvedDestination = session.findDestination(topic, DestinationType.TOPIC).get(1, TimeUnit.SECONDS);
    destinationCache.put(topic, resolvedDestination);
    return resolvedDestination;
  }

  private String resolveTopic(String twinId, EntityTwin twin) {
    String resolvedTopic = topicTemplate.replace("{twinId}", twinId);

    if (twin != null && twin.getClass().getSimpleName() != null) {
      resolvedTopic = resolvedTopic.replace("{twinType}", twin.getClass().getSimpleName());
    }
    else {
      resolvedTopic = resolvedTopic.replace("{twinType}", "EntityTwin");
    }

    return resolvedTopic;
  }

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("twin_json_publisher", this);
    sessionContextBuilder.setResetState(true)
        .setSessionExpiry(0)
        .isInternal(true)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(sessionContextBuilder.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return "twin_json_publisher";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
    // no-op
  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return "twin_json_publisher";
  }

  @Override
  public String getProtocolName() {
    return "internal";
  }

  @Override
  public String getRemoteIp() {
    return "";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // no-op
  }
}