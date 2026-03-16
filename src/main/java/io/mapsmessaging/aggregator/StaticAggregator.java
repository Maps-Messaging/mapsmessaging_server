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
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.WindowCloseMode;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class StaticAggregator implements Aggregator, ClientConnection, MessageListener, ProcessedHandler {

  private final AggregatorConfigDTO configDTO;

  private final Logger logger = LoggerFactory.getLogger(StaticAggregator.class);
  private final AggregatorWorkScheduler aggregatorWorkScheduler;
  private final StreamHandler[] handlers;
  private final Map<String, Integer> topicIndexMap;
  private final AggregationStrategy strategy;
  private final List<InterServerTransformation> transformation;
  private final WindowCloseMode windowCloseMode;
  private final boolean subscribeToInputs;

  private Session session;
  private Destination outboundDestination;

  private AggregatorMailbox<AggregatorEnvelope> mailbox;
  private AggregatorWorker worker;

  public StaticAggregator(AggregatorWorkScheduler aggregatorWorkScheduler, AggregatorConfigDTO configDTO) {
    this(aggregatorWorkScheduler, configDTO, true);
  }

  public StaticAggregator(AggregatorWorkScheduler aggregatorWorkScheduler, AggregatorConfigDTO configDTO, boolean subscribeToInputs) {
    this.configDTO = configDTO;
    this.aggregatorWorkScheduler = aggregatorWorkScheduler;
    this.windowCloseMode = configDTO.getWindowCloseMode();
    this.subscribeToInputs = subscribeToInputs;
    this.handlers = new StreamHandler[configDTO.getInputs().size()];
    this.topicIndexMap = new ConcurrentHashMap<>();
    this.strategy = new EnvelopeAggregationStrategy();

    int index = 0;
    for (AggregatorInputConfigDTO input : configDTO.getInputs()) {
      this.handlers[index] = new StreamHandler(input);
      this.topicIndexMap.put(input.getTopicName(), index);
      index++;
    }
    this.transformation = new ArrayList<>();
  }

  @Override
  public void start() {
    try {
      if (configDTO.getOutputTransformers() != null && !configDTO.getOutputTransformers().isEmpty()) {
        for (TransformationConfigDTO dto : configDTO.getOutputTransformers()) {
          InterServerTransformation transformer = TransformerManager.getInstance().get(dto);
          if (transformer != null) {
            transformation.add(transformer);
          }
        }
      }

      mailbox = new QueueBackedMpscMailbox<>(8192);
      session = createSession();
      outboundDestination = session.findDestination(configDTO.getOutputTopic(), DestinationType.TOPIC).get();

      if (subscribeToInputs) {
        for (StreamHandler handler : handlers) {
          handler.start(session);
        }
      }

      worker = new AggregatorWorker(
          getName(),
          mailbox,
          handlers,
          configDTO.getTimeoutMs(),
          this,
          windowCloseMode
      );
      aggregatorWorkScheduler.register(worker);
      logger.log(AGGREGATOR_STARTED_, configDTO.getInputs().size());
    } catch (ExecutionException | InterruptedException | TimeoutException | IOException e) {
      logger.log(AGGREGATOR_EXCEPTION_, configDTO.getName(), e);
      Thread.currentThread().interrupt();
      stop();
    }
  }

  @Override
  public void stop() {
    transformation.clear();

    if (worker != null) {
      try {
        aggregatorWorkScheduler.unregister(worker);
      } catch (Throwable ignored) {
      }
      worker = null;
    }

    if (subscribeToInputs) {
      for (StreamHandler handler : handlers) {
        try {
          handler.stop(session);
        } catch (Throwable ignored) {
        }
      }
    }

    if (session != null) {
      try {
        SessionManager.getInstance().close(session, true);
      } catch (IOException ignored) {
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
    for (int index = 0; index < events.length; index++) {
      if (contributions[index] != null) {
        events[index] = handlers[index].process(contributions[index]).getMessage();
      }
      topics[index] = handlers[index].getTopicName();
    }

    Message message = strategy.aggregate(topics, events);
    if (!transformation.isEmpty() && message != null) {
      ParsedMessage parsedMessage = new ParsedMessage(configDTO.getOutputTopic(), message);
      for (InterServerTransformation transformer : transformation) {
        ParsedMessage transformed = transformer.transform(configDTO.getOutputTopic(), parsedMessage);
        if (transformed != null) {
          parsedMessage = transformed;
        } else {
          break;
        }
      }
      message = parsedMessage.getMessage();
    }

    try {
      if (message != null) {
        outboundDestination.storeMessage(message);
      }
    } catch (IOException e) {
      logger.log(AGGREGATOR_COMPLETION_EXCEPTION, configDTO.getName(), e);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    String resolvedTopicName = resolveTopicName(messageEvent);
    acceptResolvedEvent(resolvedTopicName, messageEvent);
  }

  public void acceptResolvedEvent(String resolvedTopicName, @NotNull @NonNull MessageEvent messageEvent) {
    logger.log(AGGREGATOR_EVENT_RECEIVED, configDTO.getName(), resolvedTopicName);

    Integer inputIndex = topicIndexMap.get(resolvedTopicName);
    if (inputIndex == null) {
      runCompletion(messageEvent);
      return;
    }

    AggregatorEnvelope envelope = new AggregatorEnvelope(inputIndex, messageEvent);
    boolean accepted = mailbox.offer(envelope);
    if (!accepted) {
      runCompletion(messageEvent);
      logger.log(AGGREGATOR_EVENT_DROPPED, configDTO.getName(), resolvedTopicName);
      return;
    }
    aggregatorWorkScheduler.signal(worker);
  }

  private String resolveTopicName(MessageEvent messageEvent) {
    String alias = null;
    if (messageEvent.getSubscription() != null && messageEvent.getSubscription().getContext() != null) {
      alias = messageEvent.getSubscription().getContext().getAlias();
    }
    if (alias != null && !alias.isBlank()) {
      return alias;
    }
    return messageEvent.getDestinationName();
  }

  private void runCompletion(MessageEvent event) {
    Runnable completionTask = event.getCompletionTask();
    if (completionTask != null) {
      try {
        completionTask.run();
      } catch (Throwable ignored) {
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
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(configDTO.getName(), this);
    sessionContextBuilder.setResetState(true)
        .setSessionExpiry(0)
        .isInternal(true)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(sessionContextBuilder.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }
}