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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.mapsmessaging.state.config.StanagConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class StanagStateSubscriber implements MessageHandler {

  private final StateLoopProtocol protocol;
  private final String taskTopic;
  private final String chatTopic;
  private final Consumer<JsonObject> taskListener;
  private final Consumer<JsonObject> chatListener;
  private final Map<String, Destination> destinationCache = new ConcurrentHashMap<>();


  public StanagStateSubscriber(@NonNull @NotNull TwinManager twinManager,  @NonNull @NotNull StanagConfig stanagConfig) throws IOException {
    this.protocol = new StateLoopProtocol(new NoOpEndPoint(1, null, new ArrayList<>()), this);
    this.taskTopic = stanagConfig.getTaskTopic();
    this.chatTopic = stanagConfig.getChatTopic();
    this.taskListener = new TaskListener(twinManager, this);
    this.chatListener = new ChatListener(protocol);
  }

  public void start() throws IOException {
    if (!isConfigured(taskTopic) && !isConfigured(chatTopic)) {
      return;
    }

    protocol.connect(UUID.randomUUID().toString(), "anonymous", "anonymous");

    if (isConfigured(taskTopic)) {
      subscribe(taskTopic);
    }

    if (isConfigured(chatTopic) && !chatTopic.equals(taskTopic)) {
      subscribe(chatTopic);
    }
  }

  public void stop() throws IOException {
    if (isConfigured(taskTopic)) {
      protocol.unsubscribeLocal(taskTopic);
    }

    if (isConfigured(chatTopic) && !chatTopic.equals(taskTopic)) {
      protocol.unsubscribeLocal(chatTopic);
    }

    protocol.close();
  }

  public void handle(@NonNull @NotNull MessageEvent messageEvent) {
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
    messageEvent.getCompletionTask().run();
  }

  public void respond(String topicName, Message message) {
    Destination destination = destinationCache.get(topicName);
    if(destination != null && destination.isClosed()){
      destinationCache.remove(topicName);
      destination = null;
    }
    if(destination == null){
      try {
        destination = protocol.getSession().findDestination(topicName, DestinationType.TOPIC).get(1, TimeUnit.SECONDS);
        destinationCache.put(topicName, destination);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        //log it and return
      }
    }
    try {
      destination.storeMessage(message);
    } catch (IOException e) {
      // log this
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
    }
    catch (RuntimeException runtimeException) {
      return null;
    }
  }

  private boolean matchesTopic(String configuredTopic, String destinationName) {
    if (!isConfigured(configuredTopic) || destinationName == null) {
      return false;
    }

    if (configuredTopic.equals(destinationName)) {
      return true;
    }

    return matchesWildcardTopic(configuredTopic, destinationName);
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

  private boolean isConfigured(String topicName) {
    return topicName != null && !topicName.isBlank();
  }
}