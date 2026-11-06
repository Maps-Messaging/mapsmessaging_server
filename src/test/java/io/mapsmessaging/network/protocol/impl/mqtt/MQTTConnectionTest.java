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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.security.uuid.UuidGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class MQTTConnectionTest extends MQTTBaseTest {


  void justRun(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    client.connect(options);
    byte[] payload = new byte[1024];
    for(int x=0;x<payload.length;x++){
      payload[x] = (byte)(x%128);
    }
    while(true){
      MqttMessage mqttMessage = new MqttMessage();
      mqttMessage.setQos(1);
      mqttMessage.setPayload(payload);
      client.publish("/test", mqttMessage);
    }
  }

  @DisplayName("Test valid username/password MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testValidUser(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());

    options.setWill("/topic/will", "this is my will msg".getBytes(), 2, true);
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @DisplayName("Test valid user/password MQTT client connection with a reset session set")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testValidUserResetState(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    options.setCleanSession(true);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

    options = getOptions(auth, version);
    client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    options.setCleanSession(false);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

  }

  @DisplayName("Ungraceful disconnect publishes Will")
  @ParameterizedTest
  @ValueSource(ints = {MQTT_3_1, MQTT_3_1_1})
  void ungracefulDisconnectPublishesWill(int version) throws Exception {
    String topic = "/topic/will/" + UuidGenerator.getInstance().generate();
    byte[] payload = "session lifecycle will".getBytes(StandardCharsets.UTF_8);
    CountDownLatch willReceived = new CountDownLatch(1);

    MqttClient subscriber =
        new MqttClient(getUrl("tcp", false), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    MqttClient publisher =
        new MqttClient(getUrl("tcp", false), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());

    try {
      subscriber.setCallback(new MqttCallback() {
        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String receivedTopic, MqttMessage message) {
          if (topic.equals(receivedTopic) && java.util.Arrays.equals(payload, message.getPayload())) {
            willReceived.countDown();
          }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
      });

      subscriber.connect(getOptions(false, version));
      subscriber.subscribe(topic, 1);

      MqttConnectOptions publisherOptions = getOptions(false, version);
      publisherOptions.setWill(topic, payload, 1, false);
      publisher.connect(publisherOptions);

      publisher.disconnectForcibly(0, 1000, false);

      Assertions.assertTrue(willReceived.await(10, TimeUnit.SECONDS), "Will message was not published after ungraceful disconnect");
    } finally {
      if (subscriber.isConnected()) {
        subscriber.disconnect();
      }
      subscriber.close();
      publisher.close();
    }
  }

  @DisplayName("Test invalid MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttTestParameters")
  void testInvalidUser(int version, String protocol, boolean auth) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UuidGenerator.getInstance().generate().toString(), version), new MemoryPersistence());
    MqttCallback callback = new MqttCallback(){

      @Override
      public void connectionLost(Throwable throwable) {
        throwable.printStackTrace();
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
      }
    };

    client.setCallback(callback);
    client.setTimeToWait(2000);
    options.setConnectionTimeout(5);
    try {
      client.connect(options);
    } catch (MqttException e) {
      // This is correct
      return;
    }
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @DisplayName("Accept MQTT connection with no credentials when anonymous access is enabled")
  @ParameterizedTest
  @ValueSource(ints = {MQTT_3_1, MQTT_3_1_1})
  void testMissingCredentialsAcceptedWhenAnonymousEnabled(int version) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(false, version);
    options.setConnectionTimeout(5);

    MqttClient client = new MqttClient(
        getUrl("tcp", true),
        getClientId(UuidGenerator.getInstance().generate().toString(), version),
        new MemoryPersistence());
    client.setTimeToWait(5000);

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }
}
