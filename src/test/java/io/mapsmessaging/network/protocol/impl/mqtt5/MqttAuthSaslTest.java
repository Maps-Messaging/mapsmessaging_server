/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientConfig;
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5Auth;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5AuthBuilder;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5EnhancedAuthBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;
import io.mapsmessaging.security.MapsSecurityProvider;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MqttAuthSaslTest extends MQTTBaseTest {
  @BeforeAll
  static void registerSecurityProvider(){
    MapsSecurityProvider.register();
  }

  @DisplayName("Test SASL MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testConnection(int version, String protocol) throws Throwable {
    SaslClient saslClient = setForSasl(null, "admin", getPassword("admin"));
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    Mqtt5EnhancedAuthMechanism mqtt5EnhancedAuthMechanism = new Mqtt5SaslAuth(saslClient);
    Mqtt5ClientBuilder builder = MqttClient.builder()
        .useMqttVersion5()
        .enhancedAuth(mqtt5EnhancedAuthMechanism)
        .identifier(UUID.randomUUID().toString())
        .serverHost("localhost")
        .serverPort(getPort(protocol, true));
    if(isSsl){
      builder.sslConfig(getConfig());
    }
    Mqtt5AsyncClient client = builder.buildAsync();
    Mqtt5ConnAck connack = client.connect().join();
    Assertions.assertEquals(connack.getReasonCode(), Mqtt5ConnAckReasonCode.SUCCESS);
    Assertions.assertTrue(saslClient.isComplete());
    String qop = (String) saslClient.getNegotiatedProperty(Sasl.QOP);
    Assertions.assertTrue(qop.startsWith("auth"), "We should have an authorised SASL session");
    client.disconnect().join();
  }

  @DisplayName("Test unknown user SASL MQTT client connection")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testInvalidUser(int version, String protocol) throws Exception {
    SaslClient saslClient = setForSasl(null, "admin", "Bad Password");
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    Mqtt5EnhancedAuthMechanism mqtt5EnhancedAuthMechanism = new Mqtt5SaslAuth(saslClient);
    Mqtt5ClientBuilder builder = MqttClient.builder()
        .useMqttVersion5()
        .enhancedAuth(mqtt5EnhancedAuthMechanism)
        .identifier(UUID.randomUUID().toString())
        .serverHost("localhost")
        .serverPort(getPort(protocol, true));
    if(isSsl){
      builder.sslConfig(getConfig());
    }
    Mqtt5AsyncClient client = builder.buildAsync();
    try {
      client.connect().join();
    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      if(cause instanceof Mqtt5ConnAckException){
        Assertions.assertTrue( cause.getMessage().contains("BAD_USER_NAME_OR_PASSWORD"));
      }
      else{
        Assertions.fail("Should have logged bad username or password");
      }
    }
  }

  @DisplayName("Test unknown authentication mechanism")
  @ParameterizedTest
  @MethodSource("mqttGetAuthUrls")
  void testInvalidMechanism(int version, String protocol) throws Exception {
    SaslClient saslClient = badMechanism(null, "admin", "Bad Password");
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    Mqtt5EnhancedAuthMechanism mqtt5EnhancedAuthMechanism = new Mqtt5SaslAuth(saslClient);
    Mqtt5ClientBuilder builder = MqttClient.builder()
        .useMqttVersion5()
        .enhancedAuth(mqtt5EnhancedAuthMechanism)
        .identifier(UUID.randomUUID().toString())
        .serverHost("localhost")
        .serverPort(getPort(protocol, true));
    if(isSsl){
      builder.sslConfig(getConfig());
    }
    Mqtt5AsyncClient client = builder.buildAsync();
    try {
      client.connect().join();
    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      if(cause instanceof Mqtt5ConnAckException){
        Assertions.assertTrue( cause.getMessage().contains("BAD_AUTHENTICATION_METHOD"));
      }
      else{
        Assertions.fail("Should have logged bad username or password");
      }
    }
  }


  private MqttClientSslConfig getConfig() throws Exception {
    // Load the truststore
    KeyStore trustStore = KeyStore.getInstance("JKS");
    try (FileInputStream trustStoreInputStream = new FileInputStream("./my-truststore.jks")) {
      trustStore.load(trustStoreInputStream, "password".toCharArray()); // Replace with your truststore password
    }

    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);

    // Load the keystore (if mutual TLS is required)
    KeyManagerFactory keyManagerFactory = null;
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (FileInputStream keyStoreInputStream = new FileInputStream("./my-keystore.jks")) {
      keyStore.load(keyStoreInputStream, "password".toCharArray()); // Replace with your keystore password
    }
    keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, "password".toCharArray()); // Replace with your key password

    // Build the SSL configuration
    return MqttClientSslConfig.builder()
        .trustManagerFactory(trustManagerFactory)
        .keyManagerFactory(keyManagerFactory)
        .protocols(List.of("TLSv1.2", "TLSv1.3")) // Specify supported TLS protocols
        .handshakeTimeout(60, TimeUnit.SECONDS) // Set handshake timeout
        //.cipherSuites(List.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")) // Optional: specify cipher suites
        .hostnameVerifier((hostname, session) -> true) // Optional: custom hostname verifier (use cautiously)
        .build();
  }

  public SaslClient badMechanism(MqttConnectionOptions options, String username, String password) throws IOException {
    String mechanism = "SCRAM-SHA-512";
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    String[] mechanisms = {mechanism};
    ClientCallbackHandler clientHandler = new ClientCallbackHandler(username, password, "servername");
    SaslClient saslClient = Sasl.createSaslClient(mechanisms, "authorizationId", "MQTT", "serverName", props, clientHandler);
    if (options != null) {
      options.setAuthMethod(mechanism);
      options.setAuthData(new byte[0]);
    }
    return saslClient;
  }

  private static class Mqtt5SaslAuth implements Mqtt5EnhancedAuthMechanism {

    private final SaslClient saslClient;
    private final boolean failMechanism;

    public Mqtt5SaslAuth(SaslClient saslClient, boolean failMechanism) {
      this.saslClient = saslClient;
      this.failMechanism = failMechanism;
    }



    public Mqtt5SaslAuth(SaslClient saslClient) {
      this(saslClient, false);
    }

    @Override
    public @NotNull MqttUtf8String getMethod() {
      if(failMechanism){
        return MqttUtf8String.of("SCRAM-SHA-512");
      }
      return MqttUtf8String.of(saslClient.getMechanismName());
    }

    @Override
    public int getTimeout() {
      return 60_000;
    }

    @Override
    public @NotNull CompletableFuture<Void> onAuth(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5Connect mqtt5Connect, @NotNull Mqtt5EnhancedAuthBuilder mqtt5EnhancedAuthBuilder) {
      mqtt5EnhancedAuthBuilder.data(new byte[0]);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<Void> onReAuth(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5AuthBuilder mqtt5AuthBuilder) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> onContinue(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5Auth mqtt5Auth, @NotNull Mqtt5AuthBuilder mqtt5AuthBuilder) {
      try {
        ByteBuffer byteBuffer = mqtt5Auth.getData().get();
        byte[] challenge = new byte[byteBuffer.remaining()];
        byteBuffer.get(challenge);
        byte[] client = saslClient.evaluateChallenge(challenge);
        mqtt5AuthBuilder.data(client);
        if (saslClient.isComplete()) {
          return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
      } catch (SaslException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public @NotNull CompletableFuture<Boolean> onAuthSuccess(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5ConnAck mqtt5ConnAck) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> onReAuthSuccess(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5Auth mqtt5Auth) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onAuthRejected(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5ConnAck mqtt5ConnAck) {
    }

    @Override
    public void onReAuthRejected(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Mqtt5Disconnect mqtt5Disconnect) {
    }

    @Override
    public void onAuthError(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Throwable throwable) {
    }

    @Override
    public void onReAuthError(@NotNull Mqtt5ClientConfig mqtt5ClientConfig, @NotNull Throwable throwable) {
    }
  };

}
