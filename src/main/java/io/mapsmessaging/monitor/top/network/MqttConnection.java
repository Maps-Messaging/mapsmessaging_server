/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.monitor.top.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.lanterna.input.KeyStroke;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.security.uuid.UuidGenerator;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
        for (String topic : topics) {
          try {
            client.subscribe(topic, 0, this);
          } catch (MqttException e) {
            e.printStackTrace();
          }
        }
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
      synchronized (queue) {
        queue.add(mapper.readValue(jsonString, StatusMessage.class));
        queue.notify();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
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
