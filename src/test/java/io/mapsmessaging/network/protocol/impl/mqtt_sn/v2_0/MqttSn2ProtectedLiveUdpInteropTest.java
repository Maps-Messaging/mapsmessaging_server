/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.mqttsn.QoS;
import io.mapsmessaging.mqttsn.PublishOptions;
import io.mapsmessaging.mqttsn.SubscribeOptions;
import io.mapsmessaging.mqttsn.TopicRef;
import io.mapsmessaging.mqttsn.protection.ProtectionCodec;
import io.mapsmessaging.mqttsn.protection.ProtectionEnvelope;
import io.mapsmessaging.mqttsn.protection.bc.BouncyCastleProtectionProvider;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointURLFactory;
import io.mapsmessaging.network.io.impl.udp.UDPEndPointServerFactory;
import io.mapsmessaging.test.BaseTestConfig;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Live secured UDP listener, Authentication keystore, indexed counters and
 * independent mqtt-sn-2-client wire protection in both directions.
 */
class MqttSn2ProtectedLiveUdpInteropTest extends BaseTestConfig {
  @TempDir Path root;

  private static final byte[] CLIENT = HexFormat.of().parseHex("0102030405060708");
  private static final byte[] GATEWAY = HexFormat.of().parseHex("aabbccddeeff0011");
  private static final char[] STORE_PASS = "interop-store-pass".toCharArray();
  private static final char[] ENTRY_PASS = "interop-entry-pass".toCharArray();

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 0x40, 0x41, 0x42, 0x43, 0x44,
      0x45, 0x46, 0x47, 0x48, 0x49})
  void securedListenerAcceptsReferenceClientAndProtectsReplies(int scheme) throws Exception {
    Path directory = root.resolve("scheme-" + scheme);
    Files.createDirectories(directory);
    Path keyStoreFile = directory.resolve("keys.jceks");
    byte[] material = keyBytes(scheme);
    createKeyStore(keyStoreFile, scheme, material);
    int port;
    try (DatagramSocket availablePort = new DatagramSocket(0)) {
      port = availablePort.getLocalPort();
    }

    var config = new ConfigurationProperties(Map.of(
        "protection", new ConfigurationProperties(Map.of(
            "enabled", true,
            "senderIdentifier", HexFormat.of().formatHex(GATEWAY),
            "outboundScheme", scheme,
            "acceptedSchemes", Integer.toString(scheme),
            "counterDirectory", directory.resolve("counters").toString(),
            "keyStore", new ConfigurationProperties(Map.of(
                "type", "JCEKS", "store", "file",
                "path", keyStoreFile.toString(),
                "passphrase", new String(STORE_PASS))),
            "senderAliases", new ConfigurationProperties(Map.of(
                HexFormat.of().formatHex(CLIENT) + ":" + scheme, "client-key",
                HexFormat.of().formatHex(GATEWAY) + ":" + scheme, "gateway-key"))))));
    var protocolConfig = new MqttSnConfig(config, () -> ENTRY_PASS.clone());
    EndPointServerConfigDTO endpoint = new EndPointServerConfigDTO();
    endpoint.setName("mqtt-sn-secure-test-" + scheme);
    endpoint.setUrl("udp://127.0.0.1:" + port + "/");
    endpoint.setProtocolConfigs(List.of(protocolConfig));
    EndPointServerConfigDTO existing = NetworkManagerConfig.getInstance()
        .getEndPointServerConfigList().stream()
        .filter(candidate -> candidate.getProtocolConfig("mqtt-sn") != null)
        .findFirst().orElseThrow();
    endpoint.setEndPointConfig(existing.getEndPointConfig());
    endpoint.setAuthenticationRealm(existing.getAuthenticationRealm());

    var provider = new BouncyCastleProtectionProvider(context -> material.clone());
    try (EndPointManager secured = new EndPointManager(
        EndPointURLFactory.getInstance().create(endpoint.getUrl()),
        new UDPEndPointServerFactory(), endpoint, null);
         DatagramSocket client = new DatagramSocket()) {
      secured.start();
      client.setSoTimeout(3_000);
      InetSocketAddress remote = new InetSocketAddress("127.0.0.1", port);
      byte[] connect = MqttSnCodec.encodeConnect(
          new ConnectOptions(true, false, false, 1, 60, 0,
              "secure-" + scheme + "-" + System.nanoTime()));
      byte[] wrapped = wrap(provider, scheme, 1, connect);
      send(client, remote, wrapped);
      byte[] connAckWire = receive(client);
      var connAck = ProtectionCodec.decode(connAckWire, provider).mqttSnPacket();
      assertEquals(PacketType.CONNACK, MqttSnCodec.decode(ByteBuffer.wrap(connAck)).type(),
          "Secured listener must encrypt/authenticate CONNACK scheme " + scheme);

      // Authenticate a second request and response through the same real UDP session.
      send(client, remote, wrap(provider, scheme, 2, MqttSnCodec.encodePingReq(2)));
      var ping = ProtectionCodec.decode(receive(client), provider).mqttSnPacket();
      assertEquals(PacketType.PINGRESP, MqttSnCodec.decode(ByteBuffer.wrap(ping)).type());

      // Complete subscription and data delivery through the protected session.
      String topic = "v2/secure/" + scheme + "/" + System.nanoTime();
      send(client, remote, wrap(provider, scheme, 3,
          MqttSnCodec.encodeSubscribe(new SubscribeOptions(
              7, TopicRef.filter(topic), 0, false, QoS.AT_MOST_ONCE, false))));
      var suback = ProtectionCodec.decode(receive(client), provider).mqttSnPacket();
      assertEquals(PacketType.SUBACK, MqttSnCodec.decode(ByteBuffer.wrap(suback)).type());
      byte[] payload = ("secure-" + scheme).getBytes(java.nio.charset.StandardCharsets.UTF_8);
      send(client, remote, wrap(provider, scheme, 4,
          MqttSnCodec.encodePublish(new PublishOptions(
              QoS.AT_MOST_ONCE, false, false, 0, TopicRef.name(topic), payload))));
      var data = ProtectionCodec.decode(receive(client), provider).mqttSnPacket();
      var publish = MqttSnCodec.decode(ByteBuffer.wrap(data));
      assertEquals(PacketType.PUBLISH, publish.type());
      assertArrayEquals(payload, MqttSnCodec.decodePublish(publish).payload());

      // QoS 1 must produce a protected PUBACK and deliver through routing.
      send(client, remote, wrap(provider, scheme, 5,
          MqttSnCodec.encodePublish(new PublishOptions(
              QoS.AT_LEAST_ONCE, false, false, 20, TopicRef.name(topic), payload))));
      var firstQ1 = MqttSnCodec.decode(ByteBuffer.wrap(
          ProtectionCodec.decode(receive(client), provider).mqttSnPacket()));
      var secondQ1 = MqttSnCodec.decode(ByteBuffer.wrap(
          ProtectionCodec.decode(receive(client), provider).mqttSnPacket()));
      java.util.Set<PacketType> q1Types = java.util.Set.of(firstQ1.type(), secondQ1.type());
      assertEquals(java.util.Set.of(PacketType.PUBACK, PacketType.PUBLISH), q1Types);
      var q1Delivered = firstQ1.type() == PacketType.PUBLISH ? firstQ1 : secondQ1;
      assertArrayEquals(payload, MqttSnCodec.decodePublish(q1Delivered).payload());

      // QoS 2 uses PUBREC/PUBREL/PUBCOMP over authenticated UDP.
      send(client, remote, wrap(provider, scheme, 6,
          MqttSnCodec.encodePublish(new PublishOptions(
              QoS.EXACTLY_ONCE, false, false, 21, TopicRef.name(topic), payload))));
      var rec = MqttSnCodec.decode(ByteBuffer.wrap(
          ProtectionCodec.decode(receive(client), provider).mqttSnPacket()));
      assertEquals(PacketType.PUBREC, rec.type());
      assertEquals(21, MqttSnCodec.decodeAck(rec).packetIdentifier());
      send(client, remote, wrap(provider, scheme, 7,
          MqttSnCodec.encodeAck(PacketType.PUBREL, 21, null)));
      var firstQ2 = MqttSnCodec.decode(ByteBuffer.wrap(
          ProtectionCodec.decode(receive(client), provider).mqttSnPacket()));
      var secondQ2 = MqttSnCodec.decode(ByteBuffer.wrap(
          ProtectionCodec.decode(receive(client), provider).mqttSnPacket()));
      assertEquals(java.util.Set.of(PacketType.PUBCOMP, PacketType.PUBLISH),
          java.util.Set.of(firstQ2.type(), secondQ2.type()));
      var q2Delivered = firstQ2.type() == PacketType.PUBLISH ? firstQ2 : secondQ2;
      assertArrayEquals(payload, MqttSnCodec.decodePublish(q2Delivered).payload());

      // Duplicate authenticated sender counter cannot be applied twice.
      client.setSoTimeout(350);
      send(client, remote, wrapped);
      assertThrows(SocketTimeoutException.class, () -> receive(client));
      // Plaintext traffic must also be rejected on this protected session.
      send(client, remote, MqttSnCodec.encodePingReq(3));
      assertThrows(SocketTimeoutException.class, () -> receive(client));
      // Cryptographically valid frames from unknown senders must not be accepted.
      var unknownSender = new ProtectionEnvelope(scheme, 1, new byte[8],
          new byte[]{7, 8, 9, 10}, new byte[0],
          ByteBuffer.allocate(4).putInt(8).array(), MqttSnCodec.encodePingReq(6));
      send(client, remote, ProtectionCodec.encode(unknownSender, provider));
      assertThrows(SocketTimeoutException.class, () -> receive(client));

      // The right sender identity with a different symmetric key also fails.
      byte[] wrongMaterial = material.clone();
      wrongMaterial[0] ^= 0x7f;
      var wrongKeyProvider = new BouncyCastleProtectionProvider(ctx -> wrongMaterial);
      send(client, remote, wrap(wrongKeyProvider, scheme, 9, MqttSnCodec.encodePingReq(7)));
      assertThrows(SocketTimeoutException.class, () -> receive(client));

      // Corrupt ciphertext/tag must never return an authenticated response.
      byte[] corrupt = wrap(provider, scheme, 10, MqttSnCodec.encodePingReq(4));
      corrupt[corrupt.length - 1] ^= 1;
      send(client, remote, corrupt);
      assertThrows(SocketTimeoutException.class, () -> receive(client));
    }
  }

  private static byte[] wrap(BouncyCastleProtectionProvider provider, int scheme,
      int counter, byte[] inner) {
    return ProtectionCodec.encode(new ProtectionEnvelope(scheme, 1, CLIENT,
        new byte[]{11, 12, 13, 14}, new byte[0],
        ByteBuffer.allocate(4).putInt(counter).array(), inner), provider);
  }

  private static byte[] keyBytes(int scheme) {
    int bytes = switch (scheme) {
      case 0, 1 -> 32;
      case 2, 0x40, 0x43, 0x46 -> 16;
      case 3, 0x41, 0x44, 0x47 -> 24;
      case 4, 0x42, 0x45, 0x48, 0x49 -> 32;
      default -> throw new IllegalArgumentException("Unsupported scheme " + scheme);
    };
    byte[] key = new byte[bytes];
    for (int i = 0; i < bytes; i++) key[i] = (byte) (i + 1);
    return key;
  }

  private static void createKeyStore(Path file, int scheme, byte[] material) throws Exception {
    KeyStore store = KeyStore.getInstance("JCEKS");
    store.load(null, STORE_PASS);
    String algorithm = switch (scheme) {
      case 0 -> "HmacSHA256";
      case 1 -> "HmacSHA3-256";
      case 0x49 -> "ChaCha20";
      default -> "AES";
    };
    var entry = new KeyStore.SecretKeyEntry(new SecretKeySpec(material, algorithm));
    var protection = new KeyStore.PasswordProtection(ENTRY_PASS);
    store.setEntry("client-key", entry, protection);
    store.setEntry("gateway-key", entry, protection);
    try (var output = Files.newOutputStream(file)) {
      store.store(output, STORE_PASS);
    }
  }

  private static void send(DatagramSocket socket, InetSocketAddress destination, byte[] data)
      throws Exception {
    socket.send(new DatagramPacket(data, data.length, destination));
  }

  private static byte[] receive(DatagramSocket socket) throws Exception {
    byte[] buffer = new byte[65535];
    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
    socket.receive(datagram);
    return java.util.Arrays.copyOf(datagram.getData(), datagram.getLength());
  }
}
