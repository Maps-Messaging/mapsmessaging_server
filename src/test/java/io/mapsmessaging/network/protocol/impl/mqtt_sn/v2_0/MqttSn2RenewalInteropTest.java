/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.mqttsn.AuthPacket;
import io.mapsmessaging.mqttsn.ClientState;
import io.mapsmessaging.mqttsn.ConnAck;
import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.DecodedPacket;
import io.mapsmessaging.mqttsn.DisconnectOptions;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.MqttSnSession;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.mqttsn.PublishOptions;
import io.mapsmessaging.mqttsn.PublishPacket;
import io.mapsmessaging.mqttsn.QoS;
import io.mapsmessaging.mqttsn.SubscribeOptions;
import io.mapsmessaging.mqttsn.TopicRef;
import io.mapsmessaging.mqttsn.auth.AuthenticationExchange;
import io.mapsmessaging.mqttsn.auth.SaslAuthenticationMechanism;
import io.mapsmessaging.mqttsn.udp.UdpMqttSnClient;
import io.mapsmessaging.network.protocol.impl.mqtt5.ClientCallbackHandler;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import org.junit.jupiter.api.Test;

/** MSG-325: CSD01 4.11.1.1 using the actual Java client. */
class MqttSn2RenewalInteropTest extends BaseTestConfig {

  private static final InetSocketAddress AUTH_GATEWAY = new InetSocketAddress("127.0.0.1", 1887);
  private static final Duration PACKET_TIMEOUT = Duration.ofSeconds(5);

  @Test
  void repeated_scram_renewal_keeps_the_existing_session_and_subscription() throws Exception {
    try (AuthenticatedPeer peer = authenticatedPeer()) {
      String topic = "mqttsn/renewal/" + UUID.randomUUID();
      subscribe(peer.client, topic);
      SessionImpl original = serverSession(peer.username);
      renew(peer);
      renew(peer);
      assertSame(original, serverSession(peer.username));
      assertFalse(original.isClosed());
      peer.client.send(MqttSnCodec.encodePublish(new PublishOptions(QoS.AT_MOST_ONCE, false, false, 0, TopicRef.name(topic), new byte[] {42})));
      PublishPacket delivered = MqttSnCodec.decodePublish(receive(peer.client));
      assertArrayEquals(new byte[] {42}, delivered.payload());
    }
  }

  @Test
  void publications_are_buffered_until_renewal_success() throws Exception {
    try (AuthenticatedPeer peer = authenticatedPeer();
         UdpMqttSnClient publisher = plainClient()) {
      String topic = "mqttsn/auth-pause/" + UUID.randomUUID();
      subscribe(peer.client, topic);
      peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.beginReauthentication(peer.client.session().nextPacketIdentifier())));
      AuthPacket challenge = MqttSnCodec.decodeAuth(receive(peer.client));
      publisher.send(MqttSnCodec.encodePublish(new PublishOptions(QoS.AT_LEAST_ONCE, false, false,
          publisher.session().nextPacketIdentifier(), TopicRef.name(topic), new byte[] {9})));
      assertEquals(PacketType.PUBACK, receive(publisher).type());
      assertThrows(SocketTimeoutException.class, () -> peer.client.receive(Duration.ofMillis(200), packet -> {}));
      peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.continueAuthentication(challenge, peer.client.session().nextPacketIdentifier())));
      peer.authentication.acceptAuth(MqttSnCodec.decodeAuth(receive(peer.client)));
      PublishPacket delivered = MqttSnCodec.decodePublish(receive(peer.client));
      assertArrayEquals(new byte[] {9}, delivered.payload());
      peer.client.send(MqttSnCodec.encodeAck(PacketType.PUBACK, delivered.packetIdentifier(), null));
    }
  }

  @Test
  void failed_scram_renewal_disconnects_and_deletes_the_virtual_connection() throws Exception {
    try (AuthenticatedPeer peer = authenticatedPeer()) {
      SessionImpl original = serverSession(peer.username);
      peer.password.set("incorrect-renewal-password");
      peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.beginReauthentication(peer.client.session().nextPacketIdentifier())));
      AuthPacket challenge = MqttSnCodec.decodeAuth(receive(peer.client));
      peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.continueAuthentication(challenge, peer.client.session().nextPacketIdentifier())));
      DecodedPacket disconnected = receive(peer.client);
      assertEquals(PacketType.DISCONNECT, disconnected.type());
      assertEquals(0x87, MqttSnCodec.decodeDisconnect(disconnected).reasonCode());
      assertEquals(ClientState.DISCONNECTED, peer.client.session().state());
      WaitForState.waitFor(5, TimeUnit.SECONDS, original::isClosed);
      assertTrue(original.isClosed());
    }
  }

  @Test
  void abandoned_scram_renewal_times_out_and_deletes_the_connection() throws Exception {
    try (AuthenticatedPeer peer = authenticatedPeer(1)) {
      SessionImpl original = serverSession(peer.username);
      peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.beginReauthentication(peer.client.session().nextPacketIdentifier())));
      assertEquals(PacketType.AUTH, receive(peer.client).type());
      DecodedPacket disconnected = receive(peer.client);
      assertEquals(PacketType.DISCONNECT, disconnected.type());
      assertEquals(0x87, MqttSnCodec.decodeDisconnect(disconnected).reasonCode());
      WaitForState.waitFor(5, TimeUnit.SECONDS, original::isClosed);
      assertTrue(original.isClosed());
    }
  }

  @Test
  void changed_authentication_method_is_rejected_by_the_server() throws Exception {
    try (AuthenticatedPeer peer = authenticatedPeer()) {
      byte[] invalid = MqttSnCodec.encodeAuth(new AuthPacket(peer.client.session().nextPacketIdentifier(), 0x19, "OTHER", new byte[0]));
      // Bypass the client guard deliberately to test validation at the server boundary.
      peer.socket.send(new DatagramPacket(invalid, invalid.length, AUTH_GATEWAY));
      DecodedPacket disconnected = receive(peer.client);
      assertEquals(PacketType.DISCONNECT, disconnected.type());
      assertEquals(0x8C, MqttSnCodec.decodeDisconnect(disconnected).reasonCode());
      assertEquals(ClientState.DISCONNECTED, peer.client.session().state());
    }
  }







  private static void subscribe(UdpMqttSnClient client, String topic) throws Exception {
    client.send(MqttSnCodec.encodeSubscribe(new SubscribeOptions(client.session().nextPacketIdentifier(), TopicRef.filter(topic), 0, false, QoS.AT_LEAST_ONCE, false)));
    assertEquals(PacketType.SUBACK, receive(client).type());
  }

  private static void renew(AuthenticatedPeer peer) throws Exception {
    peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.beginReauthentication(peer.client.session().nextPacketIdentifier())));
    for (int round = 0; round < 4; round++) {
      DecodedPacket packet = receive(peer.client);
      assertEquals(PacketType.AUTH, packet.type(), "Renewal finishes with AUTH, never another CONNACK");
      AuthPacket auth = MqttSnCodec.decodeAuth(packet);
      if (auth.reasonCode() == 0) {
        peer.authentication.acceptAuth(auth);
        assertFalse(peer.authentication.active());
        assertEquals(ClientState.ACTIVE, peer.client.session().state());
        return;
      }
      peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.continueAuthentication(auth, peer.client.session().nextPacketIdentifier())));
    }
    throw new AssertionError("SCRAM renewal did not complete");
  }

  private static SessionImpl serverSession(String name) {
    return md.getSubSystemManager().getSessionManager().getSessions().stream().filter(session -> name.equals(session.getName())).findFirst().orElseThrow();
  }

  private static UdpMqttSnClient plainClient() throws Exception {
    UdpMqttSnClient client = new UdpMqttSnClient(new InetSocketAddress("127.0.0.1", 1884));
    try {
      client.send(MqttSnCodec.encodeConnect(new ConnectOptions(true, false, false, client.session().nextPacketIdentifier(), 30, 0, "publisher-" + UUID.randomUUID())));
      assertEquals(0, MqttSnCodec.decodeConnAck(receive(client)).reasonCode());
      return client;
    } catch (Exception | AssertionError failure) {
      client.close();
      throw failure;
    }
  }

  private static AuthenticatedPeer authenticatedPeer() throws Exception {
    return authenticatedPeer(30);
  }

  private static AuthenticatedPeer authenticatedPeer(int keepAliveSeconds) throws Exception {
    AuthenticatedPeer peer = new AuthenticatedPeer();
    try {
      int identifier = peer.client.session().nextPacketIdentifier();
      peer.client.send(MqttSnCodec.encodeConnect(new ConnectOptions(true, false, false, identifier, keepAliveSeconds, 0, peer.username),
          peer.authentication.method(), peer.authentication.initialResponse()));
      for (int round = 0; round < 4; round++) {
        DecodedPacket packet = receive(peer.client);
        if (packet.type() == PacketType.CONNACK) {
          ConnAck connAck = MqttSnCodec.decodeConnAck(packet);
          assertEquals(0, connAck.reasonCode());
          peer.authentication.acceptConnAck(connAck);
          return peer;
        }
        AuthPacket challenge = MqttSnCodec.decodeAuth(packet);
        peer.client.send(MqttSnCodec.encodeAuth(peer.authentication.continueAuthentication(challenge, peer.client.session().nextPacketIdentifier())));
      }
      throw new AssertionError("Initial SCRAM authentication did not complete");
    } catch (Exception | AssertionError failure) {
      peer.close();
      throw failure;
    }
  }

  private static DecodedPacket receive(UdpMqttSnClient client) throws Exception {
    AtomicReference<DecodedPacket> packet = new AtomicReference<>();
    client.receive(PACKET_TIMEOUT, packet::set);
    return packet.get();
  }

  private static final class AuthenticatedPeer implements AutoCloseable {
    private final String username = "renewal-" + UUID.randomUUID();
    private final AtomicReference<String> password = new AtomicReference<>("mqtt-sn-renewal-password");
    private final DatagramSocket socket = new DatagramSocket();
    private final UdpMqttSnClient client = new UdpMqttSnClient(socket, AUTH_GATEWAY, new MqttSnSession());
    private final AuthenticationExchange authentication;

    private AuthenticatedPeer() throws Exception {
      assertTrue(AuthManager.getInstance().addUser(username, password.get().toCharArray(), SessionPrivileges.create(username), new String[] {"everyone"}));
      authentication = new AuthenticationExchange(new SaslAuthenticationMechanism(() -> {
        try {
          SaslClient mechanism = Sasl.createSaslClient(new String[] {"SCRAM-SHA-256"}, null, "mqtt-sn", "localhost", Map.of(Sasl.QOP, "auth"),
              new ClientCallbackHandler(username, password.get(), "localhost"));
          if (mechanism == null) {
            throw new IllegalStateException("SCRAM-SHA-256 provider unavailable");
          }
          return mechanism;
        } catch (Exception failure) {
          throw new IllegalStateException("Unable to construct SCRAM client", failure);
        }
      }));
    }

    @Override
    public void close() throws Exception {
      try {
        if (client.session().state() != ClientState.DISCONNECTED) {
          client.send(MqttSnCodec.encodeDisconnect(new DisconnectOptions(null, null, null, "")));
        }
      } finally {
        client.close();
        try {
          authentication.close();
        } finally {
          AuthManager.getInstance().delUser(username);
        }
      }
    }
  }
}
