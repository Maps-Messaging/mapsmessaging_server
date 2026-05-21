package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class MavlinkViaUdpTest extends BaseTestConfig {

  /**
   * Adjust this to whatever namespace/topic your MAVLink UDP listener publishes to.
   * Examples you might be using:
   *  - "/udp/mavlink/#"
   *  - "/mavlink/#"
   *  - "/udp/14550/#"
   */
  private static final String SUBSCRIBE_TOPIC = "/mavlink/#";

  private static final String UDP_HOST = "127.0.0.1";
  private static final int UDP_PORT = 14550;

  @Test
  void testSimpleSendHeartbeatOverUdp() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch firstMessageLatch = new CountDownLatch(1);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      messages.add(messageEvent.getMessage());
      firstMessageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("mavlinkUdpSimpleTest" + System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder =
          new SubscriptionContextBuilder(SUBSCRIBE_TOPIC, ClientAcknowledgement.AUTO);

      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);

      byte[] heartbeat = buildMavlinkV1HeartbeatFrame(
          1,    // seq
          1,    // sysid
          1     // compid
      );

      sendUdpDatagram(UDP_HOST, UDP_PORT, heartbeat);

      boolean received = firstMessageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "No MAVLink messages were published after UDP injection to port " + UDP_PORT);
      Assertions.assertTrue(receivedCount.get() > 0, "Expected at least one message after UDP injection");

      int attempts = 0;
      do {
        delay(100);
        attempts++;
      } while (messages.isEmpty() && attempts < 20);

      session.removeSubscription(context.getKey());

      Assertions.assertEquals(1, messages.size(), "Expected exactly one message after single heartbeat injection");
      validateMavlinkJsonPayload(messages.getFirst(), heartbeat);
    } finally {
      close(session);
    }
  }

  private void validateMavlinkJsonPayload(Message message, byte[] expectedBinary) {
    Assertions.assertNotNull(message, "Message must not be null");

    byte[] payloadBytes = message.getOpaqueData();
    Assertions.assertNotNull(payloadBytes, "Message payload must not be null");
    Assertions.assertTrue(payloadBytes.length > 0, "Message payload must not be empty");

    String payload = new String(payloadBytes, StandardCharsets.UTF_8).trim();
    Assertions.assertFalse(payload.isEmpty(), "Message payload must not be blank");

    JsonElement root;
    try {
      root = JsonParser.parseString(payload);
    } catch (Exception e) {
      Assertions.fail("Payload is not valid JSON. Payload: " + payload, e);
      return;
    }

    Assertions.assertTrue(root.isJsonObject(), "Payload must be a JSON object. Payload: " + payload);
    JsonObject object = root.getAsJsonObject();

    assertFieldPresent(object, "mavlink");
    Assertions.assertTrue(object.get("mavlink").isJsonObject(), "mavlink must be an object. Payload: " + payload);
    JsonObject mavlink = object.getAsJsonObject("mavlink");

    assertFieldPresent(mavlink, "version");
    assertFieldPresent(mavlink, "messageId");
    assertFieldPresent(mavlink, "systemId");
    assertFieldPresent(mavlink, "componentId");
    assertFieldPresent(mavlink, "sequence");
    assertFieldPresent(mavlink, "payloadLength");
    assertFieldPresent(mavlink, "signed");
    assertFieldPresent(mavlink, "payload");

    Assertions.assertEquals("V1", mavlink.get("version").getAsString(), "Expected MAVLink V1. Payload: " + payload);
    Assertions.assertEquals(0, mavlink.get("messageId").getAsInt(), "Expected HEARTBEAT msgId=0. Payload: " + payload);
    Assertions.assertEquals(1, mavlink.get("systemId").getAsInt(), "systemId mismatch. Payload: " + payload);
    Assertions.assertEquals(1, mavlink.get("componentId").getAsInt(), "componentId mismatch. Payload: " + payload);
    Assertions.assertTrue(mavlink.get("sequence").getAsInt() >= 0 && mavlink.get("sequence").getAsInt() <= 255, "sequence must be [0..255]. Payload: " + payload);
    Assertions.assertEquals(9, mavlink.get("payloadLength").getAsInt(), "payloadLength mismatch. Payload: " + payload);
    Assertions.assertFalse(mavlink.get("signed").getAsBoolean(), "V1 heartbeat should not be signed. Payload: " + payload);

    JsonObject payloadObject = mavlink.getAsJsonObject("payload");
    assertFieldPresent(payloadObject, "rawBase64");

    String rawBase64 = payloadObject.get("rawBase64").getAsString();
    Assertions.assertFalse(rawBase64.isBlank(), "rawBase64 must not be blank. Payload: " + payload);

    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(rawBase64);
    } catch (IllegalArgumentException e) {
      Assertions.fail("rawBase64 is not valid Base64. rawBase64=" + rawBase64 + " Payload: " + payload, e);
      return;
    }

    // Your JSON says payloadLength=9, so rawBase64 should decode to 9 bytes.
    Assertions.assertEquals(9, decoded.length, "Decoded rawBase64 length must match payloadLength. Payload: " + payload);

    // Optional but useful: check the decoded payload bytes match what we injected.
    // expectedBinary is the full MAVLink frame; payload bytes start after 6 header bytes for v1.
    byte[] expectedPayload = extractMavlinkV1Payload(expectedBinary);
    Assertions.assertArrayEquals(expectedPayload, decoded, "Decoded MAVLink payload does not match injected frame payload. Payload: " + payload);

    // Validate "decoded" fields if present
    if (payloadObject.has("decoded") && payloadObject.get("decoded").isJsonObject()) {
      JsonObject decodedObject = payloadObject.getAsJsonObject("decoded");

      Assertions.assertEquals(3, decodedObject.get("mavlink_version").getAsInt(), "mavlink_version mismatch");
      Assertions.assertEquals(0, decodedObject.get("autopilot").getAsInt(), "autopilot mismatch");
      Assertions.assertEquals(0, decodedObject.get("system_status").getAsInt(), "system_status mismatch");
      Assertions.assertEquals(0, decodedObject.get("custom_mode").getAsInt(), "custom_mode mismatch");
      Assertions.assertEquals(0, decodedObject.get("base_mode").getAsInt(), "base_mode mismatch");
      Assertions.assertEquals(0, decodedObject.get("type").getAsInt(), "type mismatch");
    }
  }

  private byte[] extractMavlinkV1Payload(byte[] mavlinkFrame) {
    Assertions.assertTrue(mavlinkFrame.length >= 6, "MAVLink frame too short");
    int magic = mavlinkFrame[0] & 0xFF;
    Assertions.assertEquals(0xFE, magic, "Expected MAVLink v1 magic 0xFE");
    int payloadLength = mavlinkFrame[1] & 0xFF;
    Assertions.assertTrue(mavlinkFrame.length >= 6 + payloadLength + 2, "MAVLink frame length does not match header payload length");

    byte[] payload = new byte[payloadLength];
    System.arraycopy(mavlinkFrame, 6, payload, 0, payloadLength);
    return payload;
  }


  private void sendUdpDatagram(String host, int port, byte[] payload) throws IOException {
    InetAddress address = InetAddress.getByName(host);
    DatagramPacket packet = new DatagramPacket(payload, payload.length, address, port);

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.send(packet);
    }
  }

  /**
   * MAVLink v1 HEARTBEAT message (msgid 0), payload length 9.
   *
   * Frame format:
   *  magic(0xFE), len, seq, sysid, compid, msgid, payload[9], checksum[2]
   *
   * CRC is X25 over: len..payload plus "CRC extra" byte for that msg type.
   * For HEARTBEAT, CRC extra is 50 (0x32).
   */
  private byte[] buildMavlinkV1HeartbeatFrame(int sequence, int systemId, int componentId) {
    int payloadLength = 9;
    int magic = 0xFE;
    int msgId = 0; // HEARTBEAT

    byte[] payload = new byte[payloadLength];

    // custom_mode (uint32 little-endian)
    payload[0] = 0x00;
    payload[1] = 0x00;
    payload[2] = 0x00;
    payload[3] = 0x00;

    // type (uint8) - MAV_TYPE_GENERIC(0) is fine
    payload[4] = 0x00;

    // autopilot (uint8) - MAV_AUTOPILOT_GENERIC(0)
    payload[5] = 0x00;

    // base_mode (uint8)
    payload[6] = 0x00;

    // system_status (uint8)
    payload[7] = 0x00;

    // mavlink_version (uint8) typically 3
    payload[8] = 0x03;

    byte[] frame = new byte[6 + payloadLength + 2];

    frame[0] = (byte) magic;
    frame[1] = (byte) payloadLength;
    frame[2] = (byte) (sequence & 0xFF);
    frame[3] = (byte) (systemId & 0xFF);
    frame[4] = (byte) (componentId & 0xFF);
    frame[5] = (byte) (msgId & 0xFF);

    System.arraycopy(payload, 0, frame, 6, payloadLength);

    int crc = 0xFFFF;

    crc = x25CrcAccumulate((byte) payloadLength, crc);
    crc = x25CrcAccumulate((byte) (sequence & 0xFF), crc);
    crc = x25CrcAccumulate((byte) (systemId & 0xFF), crc);
    crc = x25CrcAccumulate((byte) (componentId & 0xFF), crc);
    crc = x25CrcAccumulate((byte) (msgId & 0xFF), crc);

    for (int i = 0; i < payloadLength; i++) {
      crc = x25CrcAccumulate(payload[i], crc);
    }

    // CRC extra for HEARTBEAT is 50
    crc = x25CrcAccumulate((byte) 50, crc);

    frame[6 + payloadLength] = (byte) (crc & 0xFF);
    frame[6 + payloadLength + 1] = (byte) ((crc >> 8) & 0xFF);

    return frame;
  }

  private int x25CrcAccumulate(byte input, int crc) {
    int tmp = (input ^ (crc & 0xFF)) & 0xFF;
    tmp = (tmp ^ ((tmp << 4) & 0xFF)) & 0xFF;
    int result = ((crc >> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >> 4)) & 0xFFFF;
    return result;
  }

  private void assertFieldPresent(JsonObject object, String fieldName) {
    Assertions.assertTrue(object.has(fieldName), "Missing field '" + fieldName + "'. Object: " + object);
    Assertions.assertFalse(object.get(fieldName).isJsonNull(), "Field '" + fieldName + "' must not be null. Object: " + object);
  }

  private byte[] hexToBytes(String hex) {
    int length = hex.length();
    byte[] bytes = new byte[length / 2];

    for (int i = 0; i < length; i += 2) {
      int value = Integer.parseInt(hex.substring(i, i + 2), 16);
      bytes[i / 2] = (byte) value;
    }

    return bytes;
  }
}