/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.app.top.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.dto.rest.StatusMessageDTO;
import io.mapsmessaging.engine.system.impl.server.DestinationStatusTopic;
import io.mapsmessaging.engine.system.impl.server.InterfaceStatusTopic;
import io.mapsmessaging.security.uuid.UuidGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConnection implements IMqttMessageListener {

  private final MqttClient client;
  private final LinkedList<Object> queue;
  private final ObjectMapper mapper;
  private final List<String> topics;
  private final MqttConnectOptions options;
  private boolean online;
  private boolean initialConnect = true;

  public MqttConnection(String url, String username, String password) throws MqttException {
    queue = new LinkedList<>();
    topics = new ArrayList<>();
    mapper = new ObjectMapper();

    options = getOptions();
    if(username != null){
      options.setUserName(username);
      options.setPassword(password.toCharArray());
    }
    client = new MqttClient(url, UuidGenerator.getInstance().generate().toString(), new MemoryPersistence());
  }

  public void close() throws MqttException {
    client.disconnect();
  }

  public boolean isConnected() {
    if (initialConnect) {
      try {
        client.connect(options);
        initialConnect = false;
      } catch (MqttException e) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        return false;
      }
    }

    if (!client.isConnected()) {
      online = false;
      try {
        Thread.sleep(1000);
        client.reconnect();
      } catch (MqttException | InterruptedException e) {
        e.printStackTrace();
      }
    } else {
      if (!online) {
        online = true;
        int[] qos  = new int[topics.size()];
        IMqttMessageListener[] listeners = new IMqttMessageListener[topics.size()];
        Arrays.fill(qos, 0);
        Arrays.fill(listeners, this);
        Thread t = new Thread(() -> {
          try {
            client.subscribe(topics.toArray(new String[topics.size()]), qos, listeners);
          } catch (MqttException e) {
            e.printStackTrace();
          }
        });
        t.start();
      }
    }
    return client.isConnected();
  }

  public void subscribe(String topicName) throws MqttException {
    topics.add(topicName);
    if (!initialConnect) {
      client.subscribe(topicName, 0, this);
    }
  }

  public boolean isQueueEmpty() {
    synchronized (queue) {
      return queue.isEmpty();
    }
  }

  public Object getUpdate() {
    synchronized (queue) {
      if (!queue.isEmpty()) {
        return queue.removeFirst();
      }
    }
    return null;
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    String jsonString = new String(message.getPayload());
    try {
      Object obj = parse(jsonString);
      if(obj != null) {
        synchronized (queue) {
          queue.add(obj);
          queue.notify();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Object parse(String jsonString) throws JsonProcessingException {
    if(jsonString.contains("buildDate")) {
      return mapper.readValue(jsonString, StatusMessageDTO.class);
    }
    if(jsonString.contains("destinationStatusList")){
      return mapper.readValue(jsonString, DestinationStatusTopic.DestinationStatusMessage.class);
    }
    if (jsonString.contains("interfaceName")) {
      return mapper.readValue(jsonString, InterfaceStatusTopic.InterfaceStatusMessage.class);
    }
    return null;
  }
  private MqttConnectOptions getOptions() {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setMqttVersion(4); // 3.1.1
    options.setCleanSession(false);
    options.setMaxInflight(10);
    options.setAutomaticReconnect(true);
    options.setHttpsHostnameVerificationEnabled(false);
    options.setSSLHostnameVerifier((hostname, session) -> true);
    options.setExecutorServiceTimeout(20000);
    options.setConnectionTimeout(20000);
    options.setAutomaticReconnect(false);
    return options;
  }

}
