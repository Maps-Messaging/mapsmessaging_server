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

class MQTTAuthTest extends MQTTBaseTest {

  @BeforeAll
  static void registerSecurityProvider(){
    MapsSecurityProvider.register();
  }

  @Test
  @Disabled // Need Paho to fix AUTH issues
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
  @DisplayName("Test unknown user SASL MQTT client connection")
  void testInvalidUser() throws MqttException, SaslException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {"SCRAM-BCRYPT-SHA-512"};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("unknownUser", "This is an bcrypt password", "servername");
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
    Assertions.assertThrowsExactly(MqttException.class, () -> client.connect(options).waitForCompletion(10000));
    client.close();
  }

  @Test
  @Disabled // Need Paho to fix AUTH issues
  @DisplayName("Test bad password SASL MQTT client connection")
  void testInvalidPassword() throws MqttException, SaslException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {"SCRAM-BCRYPT-SHA-512"};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("test3", "what ever", "servername");
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
    Assertions.assertThrowsExactly(MqttException.class, () -> client.connect(options).waitForCompletion(10000));
    client.close();
  }

  @Test
  @DisplayName("Test unknown authentication mechanism")
  void testInvalidMechanism() throws MqttException, SaslException {
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {"SCRAM-BCRYPT-SHA-512"};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler("test3", "what ever", "servername");
    SaslClient saslClient =  Sasl.createSaslClient(mechanisms, "authorizationId", "MQTT", "serverName", props, clientHandler);
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setAuthMethod("SomeRandomMechanism");
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
    Assertions.assertThrowsExactly(MqttException.class, () -> client.connect(options).waitForCompletion(10000));
    client.close();
  }


}
