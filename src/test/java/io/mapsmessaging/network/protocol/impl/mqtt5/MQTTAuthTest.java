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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.security.MapsSecurityProvider;
import lombok.SneakyThrows;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class MQTTAuthTest extends MQTTBaseTest {

  @BeforeAll
  static void registerSecurityProvider(){
    MapsSecurityProvider.register();
  }

  @DisplayName("Test SASL MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testSasl(int version, String protocol) throws MqttException, IOException {
    MqttConnectionOptions options = getOptions(true);
    SaslClient saslClient = setForSasl(options, "admin", getPassword("admin"));
    MqttAsyncClient client = new MqttAsyncClient(getUrl(protocol, true), UUID.randomUUID().toString(), new MemoryPersistence());
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


      @Override
      public void authPacketArrived(int authState, MqttProperties mqttProperties) {
        try {
          byte[] response = saslClient.evaluateChallenge(mqttProperties.getAuthenticationData());
          if (authState != 0) {
            mqttProperties.setAuthenticationData(response);
            client.authenticate(authState, this, mqttProperties);
            System.err.println("sent response to auth");
          }
        } catch (SaslException e) {
          e.printStackTrace();
        } catch (MqttException e) {
          e.printStackTrace();
        }
      }
    });
    client.connect(options).waitForCompletion(20000);
    Assertions.assertTrue(client.isConnected());
    Assertions.assertTrue(saslClient.isComplete());
    String qop = (String) saslClient.getNegotiatedProperty(Sasl.QOP);
    Assertions.assertTrue(qop.startsWith("auth"), "We should have an authorised SASL session");
    client.disconnect().waitForCompletion(10000);
    Assertions.assertFalse(client.isConnected());
    client.close();
  }

  @DisplayName("Test unknown user SASL MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testInvalidUser(int version, String protocol) throws MqttException, IOException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {"SCRAM-BCRYPT-SHA-512"};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("admin1", "This is an bcrypt password", "servername");
    SaslClient saslClient =  Sasl.createSaslClient(mechanisms, "authorizationId", "MQTT", "serverName", props, clientHandler);
    MqttConnectionOptions options = getOptions(true);
    options.setAuthMethod("SCRAM-BCRYPT-SHA-512");
    options.setAuthData(saslClient.evaluateChallenge(null));
    MqttAsyncClient client = new MqttAsyncClient(getUrl(protocol, true), UUID.randomUUID().toString(), new MemoryPersistence());
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
    Assertions.assertThrowsExactly(MqttException.class, () -> client.connect(options).waitForCompletion(10000));
    client.close();
  }

  @DisplayName("Test bad password SASL MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testInvalidPassword(int version, String protocol) throws MqttException, IOException {
    MqttConnectionOptions options = getOptions(true);
    SaslClient saslClient = setForSasl(options, "admin", "bad password");
    MqttAsyncClient client = new MqttAsyncClient(getUrl(protocol, true), UUID.randomUUID().toString(), new MemoryPersistence());
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
    Assertions.assertThrowsExactly(MqttException.class, () -> client.connect(options).waitForCompletion(10000));
    client.close();
  }

  @DisplayName("Test unknown authentication mechanism")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testInvalidMechanism(int version, String protocol) throws MqttException, IOException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {"SCRAM-BCRYPT-SHA-512"};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("admin", getPassword("admin"), "servername");
    SaslClient saslClient =  Sasl.createSaslClient(mechanisms, "authorizationId", "MQTT", "serverName", props, clientHandler);
    MqttConnectionOptions options = getOptions(true);
    options.setAuthMethod("SomeRandomMechanism");
    options.setAuthData(saslClient.evaluateChallenge(null));
    MqttAsyncClient client = new MqttAsyncClient(getUrl(protocol, true), UUID.randomUUID().toString(), new MemoryPersistence());
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
    Assertions.assertThrowsExactly(MqttException.class, () -> client.connect(options).waitForCompletion(10000));
    client.close();
  }


}
