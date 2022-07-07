/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline.MessagePipeline;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class StateEngine {

  private final Logger logger;

  private final Map<String, MQTT_SNPacket> subscribeResponseMap;
  @Getter
  private final TopicAliasManager topicAliasManager;
  @Getter
  @Setter
  private int maxBufferSize = 0;
  private State currentState;
  private SessionContextBuilder sessionContextBuilder;
  private final MessagePipeline pipeline;

  public StateEngine(MQTT_SNProtocol protocol, RegisteredTopicConfiguration registeredTopicConfiguration) {
    logger = LoggerFactory.getLogger(StateEngine.class);
    subscribeResponseMap = new LinkedHashMap<>();
    pipeline = new MessagePipeline(protocol, this);
    currentState = null;
    topicAliasManager = new TopicAliasManager(registeredTopicConfiguration);
  }

  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol)
      throws IOException, MalformedException {
    return currentState.handleMQTTEvent(mqtt, session, endPoint, protocol, this);
  }

  public boolean isSubscribed(String topic) {
    return subscribeResponseMap.containsKey(topic);
  }

  public MQTT_SNPacket getPreviousResponse(String topic) {
    return subscribeResponseMap.get(topic);
  }

  public void addSubscribeResponse(String topic, MQTT_SNPacket response) {
    subscribeResponseMap.put(topic, response);
  }

  public void removeSubscribeResponse(String topic) {
    subscribeResponseMap.remove(topic);
  }

  public SessionContextBuilder getSessionContextBuilder() {
    return sessionContextBuilder;
  }

  public void setSessionContextBuilder(SessionContextBuilder sessionContextBuilder) {
    this.sessionContextBuilder = sessionContextBuilder;
  }

  public void setState(State state) {
    if (currentState != null && state != null) {
      logger.log(ServerLogMessages.MQTT_SN_STATE_ENGINE_STATE_CHANGE, currentState.getName(), state.getName());
    }
    currentState = state;
  }

  public CompletableFuture<Session> createSession(SessionContextBuilder scb, MQTT_SNProtocol protocol) {
    scb.setReceiveMaximum(1);
    return SessionManager.getInstance().createAsync(scb.build(), protocol);
  }

  public void queueMessage(@NotNull @NonNull MessageEvent messageEvent) {
    pipeline.queue(messageEvent);
  }

  public void sendPublish(MQTT_SNProtocol protocol, String destination, MQTT_SNPacket publish) {
    currentState.sendPublish(protocol, destination, publish);
  }

  public void sendNextPublish() {
    pipeline.completed();
  }

  public void sleep() {
    pipeline.pause();
    getTopicAliasManager().clear();
  }

  public void wake() {
    pipeline.resume();
  }

  public void emptyQueue(int sendSize, Runnable completion) {
    pipeline.emptyQueue(sendSize, completion);
  }

  public int getQueueSize() {
    return pipeline.size();
  }
}
