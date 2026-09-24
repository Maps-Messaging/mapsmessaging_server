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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.logging.StateLogMessages;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EventPublisher implements ClientConnection, MessageListener {

  private static final int MAX_QUEUE_SIZE = 1000;

  private final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
  private final String topic;
  private final Session session;
  private final LinkedBlockingDeque<String> queue;
  private final Thread publisherThread;
  private volatile boolean running;
  private Destination destination;

  public EventPublisher(String topic) throws ExecutionException, InterruptedException, TimeoutException {
    this.topic = topic;
    session = createSession();
    destination = session.findDestination(topic, DestinationType.TOPIC).get(1, TimeUnit.SECONDS);
    queue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
    running = true;
    publisherThread = new Thread(this::publisherLoop, "tak-event-publisher-" + topic);
    publisherThread.setDaemon(true);
    publisherThread.start();
  }

  public void close() throws IOException {
    running = false;
    publisherThread.interrupt();
    SessionManager.getInstance().close(session, true);
  }

  /**
   * Queues the event for asynchronous publishing. Must never block the calling thread: callers
   * include destination delivery/task-scheduler threads, and a synchronous store here can starve
   * their shared task pool if the destination is backed up.
   */
  public void publish(String xml) throws IOException {
    synchronized (queue) {
      if (queue.remainingCapacity() == 0) {
        queue.pollFirst();
      }
      queue.offerLast(xml);
    }
  }

  private void publisherLoop() {
    while (running) {
      String xml;
      try {
        xml = queue.takeFirst();
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        if (!running) {
          break;
        }
        continue;
      }
      storeEvent(xml);
    }
  }

  private void storeEvent(String xml) {
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(xml.getBytes())
        .setQoS(QualityOfService.AT_LEAST_ONCE)
        .setContentType("text/xml")
        .storeOffline(true)
        .setSchemaId(SchemaManager.DEFAULT_XML_SCHEMA.toString())
        .setRetain(false);
    try {
      destination.storeMessage(messageBuilder.build());
    } catch (IOException e) {
      try {
        destination = locateDestination(destination.getFullyQualifiedNamespace());
      } catch (IOException relocateException) {
        logger.log(StateLogMessages.STATE_MANAGER_TAK_EVENT_PUBLISH_FAILED, topic, relocateException.getMessage());
      }
    }
  }

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("tak_publisher", this);
    sessionContextBuilder.setResetState(true)
        .setSessionExpiry(0)
        .isInternal(true)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(sessionContextBuilder.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }

  private Destination locateDestination(String name) throws IOException{
    try {
      return destination = session.findDestination(name, DestinationType.TOPIC).get(1, TimeUnit.SECONDS);
    } catch (InterruptedException| ExecutionException|TimeoutException  e) {
      throw new IOException(e);
    }
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public String getVersion() {
    return "";
  }

  @Override
  public void sendKeepAlive() {

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
    return "";
  }

  @Override
  public String getProtocolName() {
    return "";
  }

  @Override
  public String getRemoteIp() {
    return "";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }
}
