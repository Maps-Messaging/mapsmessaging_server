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

import io.mapsmessaging.aggregator.aggregate.AggregationStrategy;
import io.mapsmessaging.aggregator.aggregate.EnvelopeAggregationStrategy;
import io.mapsmessaging.aggregator.mailbox.AggregatorMailbox;
import io.mapsmessaging.aggregator.mailbox.QueueBackedMpscMailbox;
import io.mapsmessaging.aggregator.worker.AggregatorWorkScheduler;
import io.mapsmessaging.aggregator.worker.AggregatorWorker;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.*;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class Aggregator implements ClientConnection, MessageListener, ProcessedHandler {

  private final AggregatorConfigDTO configDTO;

  private final Logger logger = LoggerFactory.getLogger(Aggregator.class);
  private final AggregatorWorkScheduler aggregatorWorkScheduler;
  private final StreamHandler[] handlers;
  private final Map<String, Integer> topicIndexMap;
  private final AggregationStrategy strategy;

  private Session session;
  private Destination outboundDestination;

  private AggregatorMailbox<AggregatorEnvelope> mailbox;
  private AggregatorWorker worker;

  public Aggregator(AggregatorWorkScheduler aggregatorWorkScheduler, AggregatorConfigDTO configDTO) {
    this.configDTO = configDTO;
    this.aggregatorWorkScheduler = aggregatorWorkScheduler;

    this.handlers = new StreamHandler[configDTO.getInputs().size()];
    this.topicIndexMap = new ConcurrentHashMap<>();
    this.strategy = new EnvelopeAggregationStrategy();
    int index = 0;
    for (AggregatorInputConfigDTO input : configDTO.getInputs()) {
      this.handlers[index] = new StreamHandler(input);
      this.topicIndexMap.put(input.getTopicName(), index);
      index++;
    }
  }

  public void start() {
    try {
      mailbox = new QueueBackedMpscMailbox<>(8192);
      session = createSession();
      outboundDestination = session.findDestination(configDTO.getOutputTopic(), DestinationType.TOPIC).get();
      for (StreamHandler handler : handlers) {
        handler.start(session);
      }
      worker = new AggregatorWorker(getName(), mailbox, handlers, configDTO.getTimeoutMs(), this);
      aggregatorWorkScheduler.register(worker);
      logger.log(AGGREGATOR_STARTED_, configDTO.getInputs().size());
    } catch (ExecutionException | InterruptedException | TimeoutException | IOException e) {
      logger.log(AGGREGATOR_EXCEPTION_, configDTO.getName(), e);
      Thread.currentThread().interrupt();
      stop();
    }
  }

  public void stop() {
    if (worker != null) {
      try {
        aggregatorWorkScheduler.unregister(worker);
      } catch (Throwable ignored) {
        // log if you care
      }
      worker = null;
    }

    for (StreamHandler handler : handlers) {
      try {
        handler.stop(session);
      } catch (Throwable ignored) {
        // log if you care
      }
    }

    if (session != null) {
      try {
        SessionManager.getInstance().close(session, true);
      } catch (IOException ignored) {
        // log this with config rich details
      } finally {
        session = null;
      }
    }
    logger.log(AGGREGATOR_STOPPED_, configDTO.getInputs().size());
  }

  @Override
  public void completed(MessageEvent[] contributions) {
    logger.log(AGGREGATOR_COMPLETED_, configDTO.getName(), configDTO.getInputs().size());
    Message[] events = new Message[contributions.length];
    String[] topics = new String[contributions.length];
    for (int i = 0; i < events.length; i++) {
      if (contributions[i] != null) {
        events[i] = handlers[i].process(contributions[i]).getMessage();
      }
      topics[i] = handlers[i].getTopicName();
    }
    Message message = strategy.aggregate (topics, events);
    try {
      outboundDestination.storeMessage(message);
    } catch (IOException e) {
      logger.log(AGGREGATOR_COMPLETION_EXCEPTION, configDTO.getName(), e);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    logger.log(AGGREGATOR_EVENT_RECEIVED, configDTO.getName(), messageEvent.getDestinationName());
    Integer inputIndex = topicIndexMap.get(messageEvent.getDestinationName());
    if (inputIndex == null) {
      runCompletion(messageEvent);
      return;
    }

    AggregatorEnvelope envelope = new AggregatorEnvelope(inputIndex, messageEvent);
    boolean accepted = mailbox.offer(envelope);
    if (!accepted) {
      runCompletion(messageEvent);
      logger.log(AGGREGATOR_EVENT_DROPPED, configDTO.getName(), messageEvent.getDestinationName());
      return;
    }
    aggregatorWorkScheduler.signal(worker);
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

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder scb = new SessionContextBuilder(configDTO.getName(), this);
    scb.setResetState(true)
        .setSessionExpiry(0)
        .isInternal(true)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(scb.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }


}
