/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.engine.session.SessionManagerTest;
import io.mapsmessaging.utilities.SystemProperties;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class ServerTopicTest extends MQTTBaseTest {

  static boolean isHardwareSupported() {
    return (md != null && md.hasDeviceManager());
  }

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test System Topics")
  void testSystemTopics(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    MqttConnectOptions options = getOptions(auth, version);
    MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UUID.randomUUID().toString(), version), new MemoryPersistence());
    AtomicInteger counter = new AtomicInteger(0);
    client.setCallback(new MqttCallback() {
      @Override
      public void connectionLost(Throwable throwable) {

      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
        counter.incrementAndGet();
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

      }
    });
    options.setCleanSession(true);
    options.setKeepAliveInterval(30);
    client.connect(options);
    client.subscribe("$SYS/#", QoS); // ALL System topics
    long endTime = System.currentTimeMillis() + 20000;
    while (counter.get() == 0 && endTime > System.currentTimeMillis()) {
      delay(10);
    }
    Assertions.assertTrue(counter.get() != 0);
    client.disconnect();
    client.close();
    endTime = System.currentTimeMillis() + 20000;
    while (SessionManagerTest.getInstance().hasIdleSessions() && endTime > System.currentTimeMillis()) {
      delay(100);
    }
  }

  @ParameterizedTest
  @MethodSource("mqttPublishTestParameters")
  @DisplayName("Test Device topics")
  @EnabledIf("isHardwareSupported")
  void testDeviceTopics(int version, String protocol, boolean auth, int QoS) throws MqttException, IOException {
    if (md.hasDeviceManager()) {
      MqttConnectOptions options = getOptions(auth, version);
      MqttClient client = new MqttClient(getUrl(protocol, auth), getClientId(UUID.randomUUID().toString(), version), new MemoryPersistence());
      AtomicInteger counter = new AtomicInteger(0);
      client.setCallback(new MqttCallback() {
        @Override
        public void connectionLost(Throwable throwable) {

        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) {
          counter.incrementAndGet();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
      });
      options.setCleanSession(true);
      options.setKeepAliveInterval(30);
      client.connect(options);
      client.subscribe("/device/#", QoS); // ALL System topics
      long endTime = System.currentTimeMillis() + 20000;
      while (counter.get() == 0 && endTime > System.currentTimeMillis()) {
        delay(10);
      }
      Assertions.assertNotEquals(counter.get(), 0);
      client.disconnect();
      client.close();
      endTime = System.currentTimeMillis() + 20000;
      while (SessionManagerTest.getInstance().hasIdleSessions() && endTime > System.currentTimeMillis()) {
        delay(100);
      }
    }
  }
}
