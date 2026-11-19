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


package io.mapsmessaging.fusion;

import io.mapsmessaging.fusion.worker.FusionWorkScheduler;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.config.aggregator.FusionConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.FusionInputConfigDTO;
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

import static io.mapsmessaging.logging.ServerLogMessages.FUSION_EXCEPTION;

public class DynamicFusionManager implements Fusion, ClientConnection, MessageListener {

  private final Logger logger = LoggerFactory.getLogger(DynamicFusionManager.class);

  private final FusionWorkScheduler aggregatorWorkScheduler;
  private final FusionConfigDTO templateConfig;

  private final StreamHandler[] wildcardHandlers;
  private final Map<String, Integer> wildcardInputIndexMap;
  private final Map<String, StaticFusion> aggregatorsByKey;
  private final Map<String, String> topicToFusionKey;
  private final Map<String, Long> aggregatorLastSeen;

  private final ScheduledExecutorService cleanupExecutor;

  private Session session;

  public DynamicFusionManager(FusionWorkScheduler aggregatorWorkScheduler, FusionConfigDTO templateConfig) {
    this.aggregatorWorkScheduler = aggregatorWorkScheduler;
    this.templateConfig = templateConfig;
    this.wildcardHandlers = new StreamHandler[templateConfig.getInputs().size()];
    this.wildcardInputIndexMap = new ConcurrentHashMap<>();
    this.aggregatorsByKey = new ConcurrentHashMap<>();
    this.topicToFusionKey = new ConcurrentHashMap<>();
    this.aggregatorLastSeen = new ConcurrentHashMap<>();
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    int index = 0;
    for (FusionInputConfigDTO inputConfig : templateConfig.getInputs()) {
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
          this::cleanupInactiveFusions,
          cleanupPeriodMs,
          cleanupPeriodMs,
          TimeUnit.MILLISECONDS
      );
    } catch (ExecutionException | InterruptedException | TimeoutException |IOException e) {
      logger.log(FUSION_EXCEPTION, templateConfig.getName(), e);
      Thread.currentThread().interrupt();
      stop();
    }
  }

  @Override
  public void stop() {
    cleanupExecutor.shutdownNow();

    for (StaticFusion aggregator : aggregatorsByKey.values()) {
      try {
        aggregator.stop();
      } catch (Throwable ignored) {
      }
    }
    aggregatorsByKey.clear();
    topicToFusionKey.clear();
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

    String aggregatorKey = topicToFusionKey.get(destinationName);
    if (aggregatorKey == null) {
      aggregatorKey = createOrResolveFusionKey(destinationName);
      if (aggregatorKey == null) {
        runCompletion(messageEvent);
        return;
      }
    }

    StaticFusion aggregator = aggregatorsByKey.get(aggregatorKey);
    if (aggregator == null) {
      runCompletion(messageEvent);
      return;
    }

    aggregatorLastSeen.put(aggregatorKey, System.currentTimeMillis());
    aggregator.acceptResolvedEvent(destinationName, messageEvent);
  }

  public void destinationDeleted(String topicName) {
    String aggregatorKey = topicToFusionKey.remove(topicName);
    if (aggregatorKey == null) {
      return;
    }

    boolean stillReferenced = topicToFusionKey.containsValue(aggregatorKey);
    if (!stillReferenced) {
      removeFusion(aggregatorKey);
    }
  }

  private String createOrResolveFusionKey(String destinationName) {
    for (FusionInputConfigDTO inputConfig : templateConfig.getInputs()) {
      String templateTopic = inputConfig.getTopicName();
      List<String> wildcardValues = matchAndExtract(templateTopic, destinationName);
      if (wildcardValues == null) {
        continue;
      }

      String aggregatorKey = buildFusionKey(wildcardValues, destinationName);
      StaticFusion existing = aggregatorsByKey.get(aggregatorKey);
      if (existing != null) {
        topicToFusionKey.put(destinationName, aggregatorKey);
        aggregatorLastSeen.put(aggregatorKey, System.currentTimeMillis());
        return aggregatorKey;
      }

      FusionConfigDTO resolvedConfig = buildResolvedConfig(aggregatorKey, wildcardValues);
      StaticFusion staticFusion = new StaticFusion(aggregatorWorkScheduler, resolvedConfig, false);
      staticFusion.start();

      StaticFusion previous = aggregatorsByKey.putIfAbsent(aggregatorKey, staticFusion);
      if (previous != null) {
        staticFusion.stop();
      }

      StaticFusion activeFusion = aggregatorsByKey.get(aggregatorKey);
      for (FusionInputConfigDTO resolvedInput : resolvedConfig.getInputs()) {
        topicToFusionKey.put(resolvedInput.getTopicName(), aggregatorKey);
      }
      aggregatorLastSeen.put(aggregatorKey, System.currentTimeMillis());

      if (activeFusion != null) {
        return aggregatorKey;
      }
      return null;
    }
    return null;
  }

  private FusionConfigDTO buildResolvedConfig(String aggregatorKey, List<String> wildcardValues) {
    FusionConfigDTO resolved = new FusionConfigDTO();
    resolved.setName(templateConfig.getName() + "-" + sanitiseKey(aggregatorKey));
    resolved.setEnabled(templateConfig.isEnabled());
    String resolvedName = templateConfig.getOutputTopic();
    if(resolvedName.contains("{topicName}")){
      resolvedName = resolvedName.replace("{topicName}", wildcardValues.get(wildcardInputIndexMap.get(templateConfig.getInputs().get(0).getTopicName())));
    }

    resolved.setOutputTopic(resolvedName);
    resolved.setWindowCloseMode(templateConfig.getWindowCloseMode());
    resolved.setWindowDurationMs(templateConfig.getWindowDurationMs());
    resolved.setTimeoutMs(templateConfig.getTimeoutMs());
    resolved.setEmitFirstEventImmediately(templateConfig.isEmitFirstEventImmediately());
    resolved.setMaxEventsPerTopic(templateConfig.getMaxEventsPerTopic());
    resolved.setOutputTransformers(templateConfig.getOutputTransformers());

    List<FusionInputConfigDTO> resolvedInputs = new ArrayList<>();
    for (FusionInputConfigDTO inputTemplate : templateConfig.getInputs()) {
      String resolvedTopic = applyWildcardValues(inputTemplate.getTopicName(), wildcardValues);
      resolvedInputs.add(copyInputTemplate(inputTemplate, resolvedTopic));
    }
    resolved.setInputs(resolvedInputs);
    return resolved;
  }

  private FusionInputConfigDTO copyInputTemplate(FusionInputConfigDTO inputTemplate, String resolvedTopic) {
    FusionInputConfigDTO copy = new FusionInputConfigDTO(inputTemplate);
    copy.setTopicName(resolvedTopic);
    return copy;
  }

  @SuppressWarnings("java:S1168") // Null denotes no topic match; an empty list denotes a successful match without wildcards.
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
          if (!tail.isEmpty()) {
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
      if ("+".equals(token) || "#".equals(token)) {
        resolved.append(wildcardValues.get(wildcardIndex));
        wildcardIndex++;
      } else {
        resolved.append(token);
      }
    }
    return resolved.toString();
  }

  private String buildFusionKey(List<String> wildcardValues, String destinationName) {
    if (wildcardValues == null || wildcardValues.isEmpty()) {
      return destinationName;
    }
    return String.join("/", wildcardValues);
  }

  private String sanitiseKey(String value) {
    return value.replace('/', '_').replace('#', '_').replace('+', '_');
  }

  private void cleanupInactiveFusions() {
    long expiryMs = Math.max(templateConfig.getTimeoutMs() * 3, 15000L);
    long now = System.currentTimeMillis();

    for (Map.Entry<String, Long> entry : aggregatorLastSeen.entrySet()) {
      String aggregatorKey = entry.getKey();
      long lastSeen = entry.getValue();
      if ((now - lastSeen) >= expiryMs) {
        removeFusion(aggregatorKey);
      }
    }
  }

  private void removeFusion(String aggregatorKey) {
    StaticFusion removed = aggregatorsByKey.remove(aggregatorKey);
    if (removed != null) {
      try {
        removed.stop();
      } catch (Throwable ignored) {
      }
    }

    aggregatorLastSeen.remove(aggregatorKey);
    topicToFusionKey.entrySet().removeIf(entry -> aggregatorKey.equals(entry.getValue()));
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
    return "DynamicFusionManager-" + templateConfig.getName();
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