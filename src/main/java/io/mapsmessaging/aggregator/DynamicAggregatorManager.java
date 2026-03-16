/*
 *
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

import io.mapsmessaging.aggregator.worker.AggregatorWorkScheduler;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.session.ClientConnection;
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

import static io.mapsmessaging.logging.ServerLogMessages.AGGREGATOR_EXCEPTION_;

public class DynamicAggregatorManager implements Aggregator, ClientConnection, MessageListener {

  private final Logger logger = LoggerFactory.getLogger(DynamicAggregatorManager.class);

  private final AggregatorWorkScheduler aggregatorWorkScheduler;
  private final AggregatorConfigDTO templateConfig;

  private final StreamHandler[] wildcardHandlers;
  private final Map<String, Integer> wildcardInputIndexMap;
  private final Map<String, StaticAggregator> aggregatorsByKey;
  private final Map<String, String> topicToAggregatorKey;
  private final Map<String, Long> aggregatorLastSeen;

  private final ScheduledExecutorService cleanupExecutor;

  private Session session;

  public DynamicAggregatorManager(AggregatorWorkScheduler aggregatorWorkScheduler, AggregatorConfigDTO templateConfig) {
    this.aggregatorWorkScheduler = aggregatorWorkScheduler;
    this.templateConfig = templateConfig;
    this.wildcardHandlers = new StreamHandler[templateConfig.getInputs().size()];
    this.wildcardInputIndexMap = new ConcurrentHashMap<>();
    this.aggregatorsByKey = new ConcurrentHashMap<>();
    this.topicToAggregatorKey = new ConcurrentHashMap<>();
    this.aggregatorLastSeen = new ConcurrentHashMap<>();
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    int index = 0;
    for (AggregatorInputConfigDTO inputConfig : templateConfig.getInputs()) {
      wildcardHandlers[index] = new StreamHandler(inputConfig);
      wildcardInputIndexMap.put(inputConfig.getTopicName(), index);
      index++;
    }
  }

  @Override
  public void start() {
    try {
      session = createSession();
      for (StreamHandler handler : wildcardHandlers) {
        handler.start(session);
      }

      long cleanupPeriodMs = Math.max(templateConfig.getTimeoutMs(), 5000L);
      cleanupExecutor.scheduleAtFixedRate(
          this::cleanupInactiveAggregators,
          cleanupPeriodMs,
          cleanupPeriodMs,
          TimeUnit.MILLISECONDS
      );
    } catch (ExecutionException | InterruptedException | TimeoutException |IOException e) {
      logger.log(AGGREGATOR_EXCEPTION_, templateConfig.getName(), e);
      Thread.currentThread().interrupt();
      stop();
    }
  }

  @Override
  public void stop() {
    cleanupExecutor.shutdownNow();

    for (StaticAggregator aggregator : aggregatorsByKey.values()) {
      try {
        aggregator.stop();
      } catch (Throwable ignored) {
      }
    }
    aggregatorsByKey.clear();
    topicToAggregatorKey.clear();
    aggregatorLastSeen.clear();

    for (StreamHandler handler : wildcardHandlers) {
      try {
        handler.stop(session);
      } catch (Throwable ignored) {
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
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    String destinationName = messageEvent.getDestinationName();
    if (destinationName == null || destinationName.isBlank()) {
      runCompletion(messageEvent);
      return;
    }

    String aggregatorKey = topicToAggregatorKey.get(destinationName);
    if (aggregatorKey == null) {
      aggregatorKey = createOrResolveAggregatorKey(destinationName);
      if (aggregatorKey == null) {
        runCompletion(messageEvent);
        return;
      }
    }

    StaticAggregator aggregator = aggregatorsByKey.get(aggregatorKey);
    if (aggregator == null) {
      runCompletion(messageEvent);
      return;
    }

    aggregatorLastSeen.put(aggregatorKey, System.currentTimeMillis());
    aggregator.acceptResolvedEvent(destinationName, messageEvent);
  }

  public void destinationDeleted(String topicName) {
    String aggregatorKey = topicToAggregatorKey.remove(topicName);
    if (aggregatorKey == null) {
      return;
    }

    boolean stillReferenced = topicToAggregatorKey.containsValue(aggregatorKey);
    if (!stillReferenced) {
      removeAggregator(aggregatorKey);
    }
  }

  private String createOrResolveAggregatorKey(String destinationName) {
    for (AggregatorInputConfigDTO inputConfig : templateConfig.getInputs()) {
      String templateTopic = inputConfig.getTopicName();
      List<String> wildcardValues = matchAndExtract(templateTopic, destinationName);
      if (wildcardValues == null) {
        continue;
      }

      String aggregatorKey = buildAggregatorKey(wildcardValues, destinationName);
      StaticAggregator existing = aggregatorsByKey.get(aggregatorKey);
      if (existing != null) {
        topicToAggregatorKey.put(destinationName, aggregatorKey);
        aggregatorLastSeen.put(aggregatorKey, System.currentTimeMillis());
        return aggregatorKey;
      }

      AggregatorConfigDTO resolvedConfig = buildResolvedConfig(aggregatorKey, wildcardValues);
      StaticAggregator staticAggregator = new StaticAggregator(aggregatorWorkScheduler, resolvedConfig, false);
      staticAggregator.start();

      StaticAggregator previous = aggregatorsByKey.putIfAbsent(aggregatorKey, staticAggregator);
      if (previous != null) {
        staticAggregator.stop();
      }

      StaticAggregator activeAggregator = aggregatorsByKey.get(aggregatorKey);
      for (AggregatorInputConfigDTO resolvedInput : resolvedConfig.getInputs()) {
        topicToAggregatorKey.put(resolvedInput.getTopicName(), aggregatorKey);
      }
      aggregatorLastSeen.put(aggregatorKey, System.currentTimeMillis());

      if (activeAggregator != null) {
        return aggregatorKey;
      }
      return null;
    }
    return null;
  }

  private AggregatorConfigDTO buildResolvedConfig(String aggregatorKey, List<String> wildcardValues) {
    AggregatorConfigDTO resolved = new AggregatorConfigDTO();
    resolved.setName(templateConfig.getName() + "-" + sanitiseKey(aggregatorKey));
    resolved.setEnabled(templateConfig.isEnabled());
    resolved.setOutputTopic(templateConfig.getOutputTopic());
    resolved.setWindowCloseMode(templateConfig.getWindowCloseMode());
    resolved.setWindowDurationMs(templateConfig.getWindowDurationMs());
    resolved.setTimeoutMs(templateConfig.getTimeoutMs());
    resolved.setMaxEventsPerTopic(templateConfig.getMaxEventsPerTopic());
    resolved.setOutputTransformers(templateConfig.getOutputTransformers());

    List<AggregatorInputConfigDTO> resolvedInputs = new ArrayList<>();
    for (AggregatorInputConfigDTO inputTemplate : templateConfig.getInputs()) {
      String resolvedTopic = applyWildcardValues(inputTemplate.getTopicName(), wildcardValues);
      resolvedInputs.add(copyInputTemplate(inputTemplate, resolvedTopic));
    }
    resolved.setInputs(resolvedInputs);
    return resolved;
  }

  private AggregatorInputConfigDTO copyInputTemplate(AggregatorInputConfigDTO inputTemplate, String resolvedTopic) {
    AggregatorInputConfigDTO copy = new AggregatorInputConfigDTO(inputTemplate);
    copy.setTopicName(resolvedTopic);
    return copy;
  }

  private List<String> matchAndExtract(String templateTopic, String destinationTopic) {
    String[] templateTokens = templateTopic.split("/", -1);
    String[] destinationTokens = destinationTopic.split("/", -1);

    List<String> extracted = new ArrayList<>();
    int templateIndex = 0;
    int destinationIndex = 0;

    while (templateIndex < templateTokens.length && destinationIndex < destinationTokens.length) {
      String templateToken = templateTokens[templateIndex];
      String destinationToken = destinationTokens[destinationIndex];

      if ("+".equals(templateToken)) {
        extracted.add(destinationToken);
        templateIndex++;
        destinationIndex++;
        continue;
      }

      if ("#".equals(templateToken)) {
        StringBuilder tail = new StringBuilder();
        for (int index = destinationIndex; index < destinationTokens.length; index++) {
          if (tail.length() > 0) {
            tail.append('/');
          }
          tail.append(destinationTokens[index]);
        }
        extracted.add(tail.toString());
        templateIndex = templateTokens.length;
        destinationIndex = destinationTokens.length;
        break;
      }

      if (!templateToken.equals(destinationToken)) {
        return null;
      }

      templateIndex++;
      destinationIndex++;
    }

    if (templateIndex == templateTokens.length && destinationIndex == destinationTokens.length) {
      return extracted;
    }

    if (templateIndex == templateTokens.length - 1 && "#".equals(templateTokens[templateIndex])) {
      extracted.add("");
      return extracted;
    }

    return null;
  }

  private String applyWildcardValues(String templateTopic, List<String> wildcardValues) {
    String[] templateTokens = templateTopic.split("/", -1);
    StringBuilder resolved = new StringBuilder();
    int wildcardIndex = 0;

    for (int index = 0; index < templateTokens.length; index++) {
      if (index > 0) {
        resolved.append('/');
      }

      String token = templateTokens[index];
      if ("+".equals(token)) {
        resolved.append(wildcardValues.get(wildcardIndex));
        wildcardIndex++;
      } else if ("#".equals(token)) {
        resolved.append(wildcardValues.get(wildcardIndex));
        wildcardIndex++;
      } else {
        resolved.append(token);
      }
    }
    return resolved.toString();
  }

  private String buildAggregatorKey(List<String> wildcardValues, String destinationName) {
    if (wildcardValues == null || wildcardValues.isEmpty()) {
      return destinationName;
    }
    return String.join("/", wildcardValues);
  }

  private String sanitiseKey(String value) {
    return value.replace('/', '_').replace('#', '_').replace('+', '_');
  }

  private void cleanupInactiveAggregators() {
    long expiryMs = Math.max(templateConfig.getTimeoutMs() * 3, 15000L);
    long now = System.currentTimeMillis();

    for (Map.Entry<String, Long> entry : aggregatorLastSeen.entrySet()) {
      String aggregatorKey = entry.getKey();
      long lastSeen = entry.getValue();
      if ((now - lastSeen) >= expiryMs) {
        removeAggregator(aggregatorKey);
      }
    }
  }

  private void removeAggregator(String aggregatorKey) {
    StaticAggregator removed = aggregatorsByKey.remove(aggregatorKey);
    if (removed != null) {
      try {
        removed.stop();
      } catch (Throwable ignored) {
      }
    }

    aggregatorLastSeen.remove(aggregatorKey);
    topicToAggregatorKey.entrySet().removeIf(entry -> aggregatorKey.equals(entry.getValue()));
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
    return "DynamicAggregatorManager-" + templateConfig.getName();
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
    return templateConfig.getName();
  }

  @Override
  public String getProtocolName() {
    return "aggregator-dynamic";
  }

  @Override
  public String getRemoteIp() {
    return "loop";
  }

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(templateConfig.getName(), this);
    sessionContextBuilder.setResetState(true)
        .setSessionExpiry(0)
        .isInternal(true)
        .setPersistentSession(false)
        .setReceiveMaximum(100);

    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(sessionContextBuilder.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }
}