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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.NatsJetStreamSubscriptions;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Owns persistent, synchronous archive subscriptions on the real MAPS topics
 * covered by JetStream streams. This does not create an extra topic or store.
 *
 * Archives are scoped to the authenticated MAPS user and JetStream stream.
 * A client disconnect must not close the archive session. Authentication is
 * inherited from the already logged-in client; no passwords are retained.
 */
public final class JetStreamArchiveManager {

  private static final long ARCHIVE_EXPIRY_SECONDS = TimeUnit.DAYS.toSeconds(365);
  private static final JetStreamArchiveManager INSTANCE = new JetStreamArchiveManager();

  private final Map<String, Archive> archives = new HashMap<>();

  private JetStreamArchiveManager() {
  }

  public static JetStreamArchiveManager getInstance() {
    return INSTANCE;
  }

  /**
   * Create or resume the archive interest before publishing or creating a consumer.
   * This is a NATS-only use of the ordinary MAPS subscription API.
   */
  public synchronized String ensureArchive(SessionState connection, String stream, String topic) throws IOException {
    String user = authenticatedUser(connection);
    if (stream == null || stream.isBlank() || topic == null || topic.isBlank()) {
      throw new IOException("JetStream archive requires a stream and MAPS topic");
    }
    String sessionId = archiveSessionId(user, stream);
    Archive archive = archives.get(sessionId);
    if (archive == null || archive.session().isClosed()) {
      Session opened = openArchive(connection, user, sessionId);
      archive = new Archive(stream, opened, new HashSet<>(NatsJetStreamSubscriptions.filters(opened)));
      archives.put(sessionId, archive);
    }

    // Always resolve the current destination. Cached topic names and restored
    // metadata can outlive a physically deleted/recreated MAPS destination.
    Destination destination = await(archive.session().findDestination(topic, DestinationType.TOPIC));
    if (destination == null) {
      throw new IOException("MAPS topic not found for JetStream archive: " + topic);
    }
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(
        topic, ClientAcknowledgement.INDIVIDUAL);
    builder.setAlias(archiveAlias(stream, topic));
    builder.setSync(true);
    builder.setRetainHandler(RetainHandler.DO_NOT_SEND);
    builder.setNoLocalMessages(false);
    builder.setReceiveMaximum(1);
    if (NatsJetStreamSubscriptions.ensure(archive.session(), destination, builder.build()) == null) {
      throw new IOException("MAPS did not register the JetStream archive for " + topic);
    }
    archive.topics().add(topic);
    return archive.session().getName();
  }

  /**
   * Stop retaining topics removed from an updated JetStream stream. The archive
   * belongs to NATS, not to the publishing TCP connection.
   */
  public synchronized void releaseTopics(String stream, Collection<String> topics) throws IOException {
    for (Iterator<Map.Entry<String, Archive>> iterator = archives.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry<String, Archive> entry = iterator.next();
      Archive archive = entry.getValue();
      if (!archive.stream().equals(stream)) {
        continue;
      }
      for (String topic : topics) {
        if (archive.topics().remove(topic)) {
          archive.session().removeSubscription(topic);
        }
      }
      if (archive.topics().isEmpty()) {
        await(SessionManager.getInstance().closeAsync(archive.session(), true));
        iterator.remove();
      }
    }
  }

  /**
   * A deleted stream must release its archive subscriptions so the persistent
   * MAPS session does not keep an orphaned destination alive.
   */
  public synchronized void releaseStream(String stream) throws IOException {
    for (Iterator<Map.Entry<String, Archive>> iterator = archives.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry<String, Archive> entry = iterator.next();
      Archive archive = entry.getValue();
      if (archive.stream().equals(stream)) {
        for (String topic : archive.topics()) {
          archive.session().removeSubscription(topic);
        }
        await(SessionManager.getInstance().closeAsync(archive.session(), true));
        iterator.remove();
      }
    }
  }

  private static String archiveAlias(String stream, String topic) {
    return "nats-archive:" + stream + ":" + topic;
  }

  static String archiveSessionId(String username, String stream) {
    String identity = username.length() + ":" + username + stream.length() + ":" + stream;
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
      return "nats-jetstream-archive-" + HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 is required by the JVM", failure);
    }
  }

  private Session openArchive(SessionState connection, String user, String sessionId) throws IOException {
    SessionContextBuilder builder = new SessionContextBuilder(sessionId,
        new NatsArchiveConnection(connection.getProtocol()));
    builder.setUsername(user);
    builder.isAuthorized(true);
    builder.setPersistentSession(true);
    builder.setSessionExpiry(ARCHIVE_EXPIRY_SECONDS);
    builder.setReceiveMaximum(1);
    // The archive is synchronous; an unexpected automatic delivery is not ACKed.
    Session session = await(SessionManager.getInstance().createAsync(builder.build(), event -> {}));
    try {
      session.login();
      session.resumeState();
      return session;
    } catch (IOException | RuntimeException failure) {
      SessionManager.getInstance().closeAsync(session, true);
      throw failure;
    }
  }

  private static String authenticatedUser(SessionState connection) throws IOException {
    if (connection == null || connection.getSession() == null || connection.getSession().isClosed()) {
      throw new IOException("An active authenticated NATS connection is required for the archive");
    }
    SecurityContext security = connection.getSession().getSecurityContext();
    if (security == null || !security.isLoggedIn() || security.getSubject() == null
        || security.getUsername() == null || security.getUsername().isBlank()) {
      throw new IOException("An authenticated MAPS user is required for the JetStream archive");
    }
    return security.getUsername();
  }

  private static <T> T await(CompletableFuture<T> future) throws IOException {
    try {
      return future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted creating JetStream archive", interrupted);
    } catch (ExecutionException | TimeoutException failure) {
      throw new IOException("Unable to initialise JetStream archive", failure);
    }
  }

  private record Archive(String stream, Session session, Set<String> topics) {
  }
}
