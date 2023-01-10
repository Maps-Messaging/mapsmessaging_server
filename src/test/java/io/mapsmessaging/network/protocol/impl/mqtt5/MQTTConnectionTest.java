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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.security.MapsSecurityProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import lombok.SneakyThrows;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MQTTConnectionTest extends MQTTBaseTest {
  @BeforeAll
  static void registerSecurityProvider(){
    MapsSecurityProvider.register();
  }

  @Test
  @DisplayName("Test anonymous MQTT client connection")
  void testAnonymous() throws MqttException {
    MqttConnectionOptions options = new MqttConnectionOptions();
    MqttClient client = new MqttClient("tcp://localhost:8675", UUID.randomUUID().toString(), new MemoryPersistence());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Test
  @Disabled // Until Paho MQTT5 supports AUTH
  @DisplayName("Test SASL MQTT client connection")
  void testSasl() throws MqttException, SaslException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {"SCRAM-BCRYPT-SHA-512"};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("test3", "This is an bcrypt password", "servername");
    SaslClient saslClient =  Sasl.createSaslClient(mechanisms, "authorizationId", "MQTT", "serverName", props, clientHandler);
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setAuthMethod("SCRAM-BCRYPT-SHA-512");
    options.setUserName("test3");
    options.setAuthData(saslClient.evaluateChallenge(null));
    MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:2883", UUID.randomUUID().toString(), new MemoryPersistence());
    client.setCallback(new MqttCallback() {
      @Override
      public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
      }

      @Override
      public void mqttErrorOccurred(MqttException e) {
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
      }

      @Override
      public void deliveryComplete(IMqttToken iMqttToken) {
      }

      @Override
      public void connectComplete(boolean b, String s) {
      }

      @SneakyThrows
      @Override
      public void authPacketArrived(int authState, MqttProperties mqttProperties) {
        byte[] response = saslClient.evaluateChallenge(mqttProperties.getAuthenticationData());
        if (authState != 0) {
          mqttProperties.setAuthenticationData(response);
          client.authenticate(authState, this, mqttProperties);
        }
      }
    });
    client.connect(options).waitForCompletion(10000);
    Assertions.assertTrue(client.isConnected());
    Assertions.assertTrue(saslClient.isComplete());
    String qop = (String) saslClient.getNegotiatedProperty(Sasl.QOP);
    Assertions.assertTrue(qop.startsWith("auth"), "We should have an authorised SASL session");
    client.disconnect().waitForCompletion(10000);
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Test
  @DisplayName("Test valid username/password MQTT client connection")
  void testValidUser() throws MqttException {
    MqttConnectionOptions options = new MqttConnectionOptions();
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttMessage mqttMessage = new MqttMessage();
    mqttMessage.setPayload("this is my will msg".getBytes());
    mqttMessage.setQos(2);
    mqttMessage.setRetained(true);
    options.setWill("/topic/will", mqttMessage);
    options.setUserName("user1");
    options.setPassword("password1".getBytes());
    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @Test
  @DisplayName("Test valid user/password MQTT client connection with a reset session set")
  void testValidUserResetState() throws MqttException {
    MqttConnectionOptions options = new MqttConnectionOptions();
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    options.setCleanStart(true);
    options.setUserName("user1");
    options.setPassword("password1".getBytes());

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

    client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    options = new MqttConnectionOptions();
    options.setCleanStart(false);
    options.setUserName("user1");
    options.setPassword("password1".getBytes());

    client.connect(options);
    Assertions.assertTrue(client.isConnected());
    client.disconnect();
    Assertions.assertFalse(client.isConnected());
    client.close();

  }

  @Test
  @DisplayName("Test invalid MQTT client connection")
  void testInvalidUser() throws MqttException {
    MqttClient client = new MqttClient("tcp://localhost:2001", UUID.randomUUID().toString(), new MemoryPersistence());
    MqttCallback callback = new MqttCallback() {

      @Override
      public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

      }

      @Override
      public void mqttErrorOccurred(MqttException e) {

      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) {
      }

      @Override
      public void deliveryComplete(IMqttToken iMqttToken) {

      }

      @Override
      public void connectComplete(boolean b, String s) {

      }

      @Override
      public void authPacketArrived(int i, MqttProperties mqttProperties) {

      }

    };

    client.setCallback(callback);
    client.setTimeToWait(2000);
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName("user1");
    options.setPassword("password2".getBytes());
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
}
