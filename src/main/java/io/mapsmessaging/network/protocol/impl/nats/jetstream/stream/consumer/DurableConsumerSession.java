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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.NatsJetStreamSubscriptions;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Durable JetStream consumers use MAPS persistent sessions, not a second cursor store.
 * Session identity includes the authenticated user to prevent cross-user cursor collisions.
 */
public final class DurableConsumerSession {

  private static final long DURABLE_SESSION_EXPIRY_SECONDS = TimeUnit.DAYS.toSeconds(365);
  private static final ConcurrentHashMap<String, SessionState> ATTACHED = new ConcurrentHashMap<>();

  private DurableConsumerSession() {
  }

  public static String sessionId(String user, String stream, String consumer) {
    if (user == null || user.isBlank() || stream == null || stream.isBlank() || consumer == null || consumer.isBlank()) {
      throw new IllegalArgumentException("Durable consumer identity must be nonempty");
    }
    String unique = user.length() + ":" + user + stream.length() + ":" + stream + consumer.length() + ":" + consumer;
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(unique.getBytes(StandardCharsets.UTF_8));
      return "nats-jetstream-durable-" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is required by the JVM", error);
    }
  }

  public static Session open(SessionState connection, String stream, String consumer) throws IOException {
    SessionContext context = authenticatedContext(connection, stream, consumer);
    String id = context.getId();
    if (ATTACHED.putIfAbsent(id, connection) != null) {
      throw new IOException("Durable consumer is already attached to an active NATS connection");
    }

    try {
      Session persistent = await(SessionManager.getInstance().createAsync(context, connection.getProtocol()));
      try {
        persistent.login();
        persistent.resumeState();
        return persistent;
      } catch (IOException | RuntimeException failedLogin) {
        SessionManager.getInstance().closeAsync(persistent, true);
        throw failedLogin;
      }
    } catch (IOException | RuntimeException failure) {
      ATTACHED.remove(id, connection);
      throw failure;
    }
  }

  /**
   * Derive a persistent consumer session from an already authenticated MAPS session.
   * No password is copied, cached, or obtained from the NATS CONNECT frame.
   * MAPS still performs its normal persistent-session permission checks at login.
   */
  static SessionContext authenticatedContext(SessionState connection, String stream, String consumer) throws IOException {
    if (connection == null) {
      throw new IOException("An authenticated NATS connection is required for durable consumers");
    }
    Session authenticated = connection.getSession();
    if (authenticated == null || authenticated.isClosed()) {
      throw new IOException("An active authenticated MAPS session is required for durable consumers");
    }

    SecurityContext securityContext = authenticated.getSecurityContext();
    if (securityContext == null || !securityContext.isLoggedIn() || securityContext.getSubject() == null) {
      throw new IOException("The NATS MAPS session has not completed authentication");
    }
    String user = securityContext.getUsername();
    if (user == null || user.isBlank()) {
      throw new IOException("The authenticated MAPS session has no user identity");
    }

    return new SessionContextBuilder(sessionId(user, stream, consumer),
        new ProtocolClientConnection(connection.getProtocol()))
        .setUsername(user)
        .isAuthorized(true)
        .setPersistentSession(true)
        .setSessionExpiry(DURABLE_SESSION_EXPIRY_SECONDS)
        .setReceiveMaximum(connection.getProtocol().getMaxReceiveSize())
        .build();
  }

  /**
   * Each ephemeral JetStream consumer owns a separate MAPS subscription session.
   * No NATS CONNECT password is retained or replayed.
   */
  public static Session openEphemeral(SessionState connection) throws IOException {
    String user = authenticatedContext(connection, "_ephemeral_", "_ephemeral_").getUsername();
    SessionContextBuilder builder = new SessionContextBuilder(
        "nats-jetstream-ephemeral-" + UUID.randomUUID(),
        new ProtocolClientConnection(connection.getProtocol()));
    builder.setUsername(user);
    builder.isAuthorized(true);
    builder.setPersistentSession(false);
    builder.setSessionExpiry(0);
    builder.setReceiveMaximum(connection.getProtocol().getMaxReceiveSize());
    Session ephemeral = await(SessionManager.getInstance().createAsync(builder.build(), connection.getProtocol()));
    try {
      ephemeral.login();
      ephemeral.resumeState();
      return ephemeral;
    } catch (IOException | RuntimeException failure) {
      SessionManager.getInstance().closeAsync(ephemeral, true);
      throw failure;
    }
  }

  public static CompletableFuture<Session> close(SessionState connection, Session durable) {
    return SessionManager.getInstance().closeAsync(durable, false).whenComplete((closed, failure) -> {
      if (failure == null) {
        ATTACHED.remove(durable.getName(), connection);
      }
    });
  }

  /**
   * Reconnect to the pre-existing MAPS subscription when its persistent cursor was restored.
   * Only initialise archive replay if no matching durable cursor exists yet.
   */
  public static SubscribedEventManager subscribe(Session persistent, String destination, String alias,
                                                 ClientAcknowledgement acknowledgement, int receiveMaximum)
      throws IOException {
    Destination bound = await(persistent.findDestination(destination.replace('.', '/'), DestinationType.TOPIC));
    if (bound == null) {
      throw new IOException("No MAPS destination for durable JetStream consumer: " + destination);
    }
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(destination.replace('.', '/'), acknowledgement);
    builder.setAlias(alias);
    builder.setSync(true);
    builder.setRetainHandler(RetainHandler.DO_NOT_SEND);
    builder.setNoLocalMessages(false);
    builder.setReceiveMaximum(receiveMaximum);
    return NatsJetStreamSubscriptions.ensure(persistent, bound, builder.build());
  }

  private static <T> T await(CompletableFuture<T> future) throws IOException {
    try {
      return future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted obtaining persistent MAPS consumer session", interrupted);
    } catch (ExecutionException | java.util.concurrent.TimeoutException failure) {
      throw new IOException("Could not initialise persistent MAPS consumer session", failure);
    }
  }
}
