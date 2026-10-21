/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.mqttsn.AuthPacket;
import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.DecodedPacket;
import io.mapsmessaging.mqttsn.DisconnectOptions;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.mqttsn.auth.AuthenticationExchange;
import io.mapsmessaging.mqttsn.auth.SaslAuthenticationMechanism;
import io.mapsmessaging.mqttsn.udp.UdpMqttSnClient;
import io.mapsmessaging.network.protocol.impl.mqtt5.ClientCallbackHandler;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import org.junit.jupiter.api.Test;

class MqttSnAuthTest extends BaseMqttSnConfig {

  @Test
  void scramAuthenticationUsesTheCsd01AuthExchange() throws Exception {
    // CSD01 MQTT-SN-3.1.2.3-1/-2, MQTT-SN-3.3.2-1,
    // MQTT-SN-3.3.3-1 and MQTT-SN-4.11.1-1..7.
    String username = "mqtt-sn-auth-" + UUID.randomUUID();
    String password = "mqtt-sn-test-password";
    AuthManager authManager = AuthManager.getInstance();
    assertTrue(authManager.addUser(username, password.toCharArray(),
        SessionPrivileges.create(username), new String[] {"everyone"}));

    Map<String, String> properties = new HashMap<>();
    properties.put(Sasl.QOP, "auth");
    ClientCallbackHandler callbackHandler = new ClientCallbackHandler(
        username, password, "localhost");
    SaslAuthenticationMechanism mechanism = new SaslAuthenticationMechanism(() -> {
      try {
        SaslClient saslClient = Sasl.createSaslClient(new String[] {"SCRAM-SHA-256"},
            null, "mqtt-sn", "localhost", properties, callbackHandler);
        if (saslClient == null) {
          throw new IllegalStateException("SCRAM-SHA-256 is not installed in the test runtime");
        }
        return saslClient;
      } catch (Exception ex) {
        throw new IllegalStateException("Unable to create the test SASL client", ex);
      }
    });

    try (AuthenticationExchange authentication = new AuthenticationExchange(mechanism);
         UdpMqttSnClient client = new UdpMqttSnClient(
             new InetSocketAddress("127.0.0.1", 1887))) {
      int connectId = client.session().nextPacketIdentifier();
      client.send(MqttSnCodec.encodeConnect(
          new ConnectOptions(true, false, false, connectId, 50, 0, username),
          authentication.method(), authentication.initialResponse()));

      boolean connected = false;
      long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
      while (!connected && System.nanoTime() < deadline) {
        DecodedPacket packet = receive(client);
        if (packet.type() == PacketType.AUTH) {
          AuthPacket challenge = MqttSnCodec.decodeAuth(packet);
          AuthPacket response = authentication.continueAuthentication(
              challenge, connectId);
          client.send(MqttSnCodec.encodeAuth(response));
        } else if (packet.type() == PacketType.CONNACK) {
          var connAck = MqttSnCodec.decodeConnAck(packet);
          assertEquals(0, connAck.reasonCode(),
              "SCRAM connection rejected with CONNACK reason 0x"
                  + Integer.toHexString(connAck.reasonCode()));
          authentication.acceptConnAck(connAck);
          connected = true;
        }
      }
      assertEquals(true, connected, "server must finish the enhanced authentication exchange");
      assertEquals("ACTIVE", client.session().state().name());
      // Cleanly close the authenticated server session before daemon teardown.
      client.send(MqttSnCodec.encodeDisconnect(new DisconnectOptions(null, null, null, "")));
      assertEquals("DISCONNECTED", client.session().state().name());
    } finally {
      authManager.delUser(username);
    }
  }

  private static DecodedPacket receive(UdpMqttSnClient client) throws Exception {
    DecodedPacket[] received = new DecodedPacket[1];
    client.receive(Duration.ofSeconds(5), packet -> received[0] = packet);
    if (received[0] == null) {
      throw new AssertionError("server returned an empty MQTT-SN datagram");
    }
    return received[0];
  }
}
