/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.DefaultConstants;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.login.LoginException;
import lombok.Getter;
import lombok.Setter;

public class StateEngine {


  private final Map<String, MQTT_SNPacket> subscribeResponseMap;
  private final HashMap<String, Short> topicAlias;
  private final AtomicInteger aliasGenerator;

  private @Getter @Setter int maxBufferSize = 0;
  private State currentState;
  private SessionContextBuilder sessionContextBuilder;

  public StateEngine() {
    subscribeResponseMap = new LinkedHashMap<>();
    topicAlias = new LinkedHashMap<>();
    currentState = null;
    aliasGenerator = new AtomicInteger(1);
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
    currentState = state;
  }

  public short getTopicAlias(String name) {
    Short alias = topicAlias.get(name);
    if (alias == null && topicAlias.size() < DefaultConstants.MAX_REGISTERED_SIZE) {
      alias = (short) aliasGenerator.incrementAndGet();
      topicAlias.put(name, alias);
    }
    if (alias == null) {
      return -1;
    }
    return alias;
  }

  public String getTopic(short alias) {
    for (Map.Entry<String, Short> entries : topicAlias.entrySet()) {
      if (entries.getValue() == alias) {
        return entries.getKey();
      }
    }
    return null;
  }

  public Session createSession(SessionContextBuilder scb, MQTT_SNProtocol protocol, MQTT_SNPacket response) throws LoginException, IOException {
    Session session = SessionManager.getInstance().create(scb.build(), protocol);
    protocol.setSession(session);
    response.setCallback(session::resumeState);
    return session;
  }

  public void sendPublish(MQTT_SNProtocol protocol, String destination, MQTT_SNPacket publish) {
    currentState.sendPublish(protocol, destination, publish);
  }

  public short findTopicAlias(String name) {
    Short alias = topicAlias.get(name);
    if (alias == null) {
      return -1;
    }
    return alias;
  }
}
