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

package io.mapsmessaging.aggregator;

import io.mapsmessaging.aggregator.mailbox.AggregatorMailbox;
import io.mapsmessaging.aggregator.mailbox.BackpressureMode;
import io.mapsmessaging.aggregator.mailbox.OfferOutcome;
import io.mapsmessaging.aggregator.mailbox.QueueBackedMpscMailbox;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.*;

public class Aggregator implements ClientConnection, MessageListener {

  private final AggregatorConfigDTO configDTO;

  private final StreamHandler[] handlers;
  private final Map<String, Integer> topicIndexMap;

  private Session session;

  private AggregatorMailbox<AggregatorEnvelope> mailbox;
  private AggregatorWorker worker;
  private Thread workerThread;

  public Aggregator(AggregatorConfigDTO configDTO) {
    this.configDTO = configDTO;

    this.handlers = new StreamHandler[configDTO.getInputs().size()];
    this.topicIndexMap = new ConcurrentHashMap<>();

    int index = 0;
    for (AggregatorInputConfigDTO input : configDTO.getInputs()) {
      this.handlers[index] = new StreamHandler(input);
      this.topicIndexMap.put(input.getTopicName(), index);
      index++;
    }
  }

  public void start() throws ExecutionException, IOException, InterruptedException, TimeoutException {
    session = createSession();

    for (StreamHandler handler : handlers) {
      handler.start(session);
    }

    this.mailbox = new QueueBackedMpscMailbox<>(8192, BackpressureMode.DROP_NEWEST);

    this.worker = new AggregatorWorker(mailbox, handlers, getTimeOut());
    this.workerThread = new Thread(worker, "AggregatorWorker-" + configDTO.getName());
    this.workerThread.setDaemon(true);
    this.workerThread.start();
  }

  public void stop() throws IOException {
    if (worker != null) {
      worker.shutdown();
    }
    if (workerThread != null) {
      try {
        workerThread.join(2000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }

    for (StreamHandler handler : handlers) {
      handler.stop(session);
    }
    SessionManager.getInstance().close(session, true);
  }

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder scb = new SessionContextBuilder(configDTO.getName(), this);
    scb.setResetState(true)
        .setSessionExpiry(0)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(scb.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Integer inputIndex = topicIndexMap.get(messageEvent.getDestinationName());
    if (inputIndex == null) {
      runCompletion(messageEvent);
      return;
    }

    AggregatorEnvelope envelope = new AggregatorEnvelope(inputIndex, messageEvent);

    OfferOutcome outcome = mailbox.offer(envelope, () -> runCompletion(messageEvent));
    if (outcome == OfferOutcome.DROPPED) {
      // TODO metrics for dropped mailbox items
    }
  }

  private void runCompletion(MessageEvent event) {
    Runnable completionTask = event.getCompletionTask();
    if (completionTask != null) {
      try {
        completionTask.run();
      } catch (Throwable ignored) {
        // Never throw from ingress.
      }
    }
  }

  @Override
  public long getTimeOut() {
    return 30000;
  }

  @Override
  public String getName() {
    return "Aggregator-" + configDTO.getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
    // No Op
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
    return configDTO.getName();
  }

  @Override
  public String getProtocolName() {
    return "aggregator";
  }

  @Override
  public String getRemoteIp() {
    return "loop";
  }
}
