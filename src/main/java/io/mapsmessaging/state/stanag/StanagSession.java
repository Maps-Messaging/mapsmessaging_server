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

package io.mapsmessaging.state.stanag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.impl.noop.NoOpEndPoint;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.StanagConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.stanag.audit.Auditor;
import io.mapsmessaging.utilities.Lifecycle;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class StanagSession implements MessageHandler, TaskMessageSender, Lifecycle {

  private final TwinManager twinManager;

  private final StateLoopProtocol protocol;
  private final String taskTopic;
  private final String chatTopic;
  private final String taskTopicTemplate;
  private final TaskListener taskListener;
  private final Consumer<JsonObject> chatListener;
  private final StanagConfig stanagConfig;
  private final Map<String, Destination> destinationCache = new ConcurrentHashMap<>();

  private NodeUpdate nodeUpdateListener;

  @Getter
  private final Auditor auditor;

  public StanagSession(@NonNull @NotNull TwinManager twinManager, @NonNull @NotNull StanagConfig stanagConfig) {
    this.twinManager = twinManager;
    this.stanagConfig = stanagConfig;
    this.protocol = new StateLoopProtocol(new NoOpEndPoint(1, null, new ArrayList<>()), this);
    this.auditor = twinManager.getAuditor();
    this.taskTopic = stanagConfig.getTaskTopic();
    this.chatTopic = stanagConfig.getChatTopic();
    this.taskTopicTemplate = stanagConfig.getTaskTopicTemplate();
    this.taskListener = new TaskListener(twinManager, this, stanagConfig);
    this.chatListener = new ChatListener(protocol);
  }

  public void start()  {
    try {
      if (!isConfigured(taskTopic) && !isConfigured(chatTopic)) {
        return;
      }
      protocol.connect(UUID.randomUUID().toString(), "anonymous", "anonymous");
      this.nodeUpdateListener = new NodeUpdate(protocol.getSession(), stanagConfig);
      if (isConfigured(taskTopic)) {
        subscribe(taskTopic);
      }
      if (isConfigured(chatTopic) && !chatTopic.equals(taskTopic)) {
        subscribe(chatTopic);
      }
      taskListener.start();
    } catch (IOException e) {
      // Log this
    }
    twinManager.addObserver(nodeUpdateListener);
  }

  public void stop() {
    taskListener.stop();
    if (isConfigured(taskTopic)) {
      protocol.unsubscribeLocal(taskTopic);
    }

    if (isConfigured(chatTopic) && !chatTopic.equals(taskTopic)) {
      protocol.unsubscribeLocal(chatTopic);
    }

    try {
      protocol.close();
    } catch (IOException e) {
      // log this
    }
  }

  @Override
  public void handle(@NonNull @NotNull MessageEvent messageEvent) {
    try {
      JsonObject jsonObject = parseJson(messageEvent.getMessage());
      if (jsonObject == null) {
        return;
      }
      String destinationName = messageEvent.getDestinationName();
      if (matchesTopic(taskTopic, destinationName)) {
        taskListener.accept(jsonObject);
      }

      if (matchesTopic(chatTopic, destinationName)) {
        chatListener.accept(jsonObject);
      }
    } finally {
      messageEvent.getCompletionTask().run();
    }
  }

  @Override
  public void sendTaskMessage(String taskTopic, Message message) {
    respond(taskTopic, message);
  }

  public void respond(String topicName, Message message) {
    Destination destination = resolveDestination(topicName);

    if (destination == null) {
      return;
    }

    try {
      destination.storeMessage(message);
    } catch (IOException exception) {
      // log this
    }
  }

  private Destination resolveDestination(String topicName) {
    destinationCache.computeIfPresent(topicName, (cachedTopicName, destination) -> destination.isClosed() ? null : destination);
    return destinationCache.computeIfAbsent(topicName, this::findDestination);
  }

  private Destination findDestination(String topicName) {
    try {
      return protocol.getSession().findDestination(topicName, DestinationType.TOPIC).get(1, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException | TimeoutException exception) {
      // log this
      return null;
    }
  }

  private void subscribe(String topicName) throws IOException {
    protocol.subscribeLocal(topicName, topicName, QualityOfService.AT_MOST_ONCE, null, null, null, null, null);
  }

  private JsonObject parseJson(Message message) {
    byte[] opaqueData = message.getOpaqueData();

    if (opaqueData == null || opaqueData.length == 0) {
      return null;
    }

    try {
      return JsonParser.parseString(new String(opaqueData, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (RuntimeException runtimeException) {
      return null;
    }
  }

  private boolean matchesTopic(String configuredTopic, String destinationName) {
    if (!isConfigured(configuredTopic) || destinationName == null) {
      return false;
    }

    return configuredTopic.equals(destinationName) || matchesWildcardTopic(configuredTopic, destinationName);
  }

  private boolean matchesWildcardTopic(String configuredTopic, String destinationName) {
    String[] configuredParts = configuredTopic.split("/");
    String[] destinationParts = destinationName.split("/");

    int configuredIndex = 0;
    int destinationIndex = 0;

    while (configuredIndex < configuredParts.length) {
      String configuredPart = configuredParts[configuredIndex];

      if ("#".equals(configuredPart)) {
        return configuredIndex == configuredParts.length - 1;
      }

      if (destinationIndex >= destinationParts.length) {
        return false;
      }

      if (!"+".equals(configuredPart) && !configuredPart.equals(destinationParts[destinationIndex])) {
        return false;
      }

      configuredIndex++;
      destinationIndex++;
    }

    return destinationIndex == destinationParts.length;
  }

  private String buildTopicName(String topicTemplate, String taskTopic) {
    return (topicTemplate + "/" + taskTopic).replaceAll("/+", "/");
  }

  private boolean isConfigured(String topicName) {
    return topicName != null && !topicName.isBlank();
  }
}