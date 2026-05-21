package io.mapsmessaging.network.protocol.impl.semtech;

import com.google.gson.JsonArray;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class SemtechViaUdpTest extends BaseTestConfig {

  /**
   * Adjust to whatever namespace/topic your Semtech UDP listener publishes to.
   * Examples:
   *  - "/semtech/#"
   *  - "/lora/semtech/#"
   *  - "/udp/semtech/#"
   */
  private static final String SUBSCRIBE_TOPIC = "/semtech/#";

  private static final String UDP_HOST = "127.0.0.1";
  private static final int UDP_PORT = 1700;

  private static final int SEMTECH_PROTOCOL_VERSION = 0x02;

  private static final int PUSH_DATA = 0x00;
  private static final int PUSH_ACK = 0x01;

  @Test
  void testPushDataOverUdpPublishesAndAcked() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch firstMessageLatch = new CountDownLatch(1);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      messages.add(messageEvent.getMessage());
      firstMessageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("semtechUdpSimpleTest" + System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(SUBSCRIBE_TOPIC, ClientAcknowledgement.AUTO);

      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);

      byte[] gatewayEui = hexToBytes("0102030405060708"); // pick any 8 bytes; used by server for routing
      int token = ThreadLocalRandom.current().nextInt(0, 0x10000);

      JsonObject pushJson = buildMinimalRxpkPushDataJson();
      byte[] pushPacket = buildPushDataPacket(token, gatewayEui, pushJson);

      try (DatagramSocket socket = new DatagramSocket()) {
        socket.setSoTimeout(2000);

        InetAddress address = InetAddress.getByName(UDP_HOST);
        DatagramPacket packet = new DatagramPacket(pushPacket, pushPacket.length, address, UDP_PORT);
        socket.send(packet);

        byte[] ackBytes = receiveUdp(socket, 64);
        validatePushAck(ackBytes, token);
      }

      boolean received = firstMessageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "No Semtech messages were published after UDP injection to port " + UDP_PORT);
      Assertions.assertTrue(receivedCount.get() > 0, "Expected at least one message after UDP injection");

      int attempts = 0;
      do {
        delay(100);
        attempts++;
      } while (messages.isEmpty() && attempts < 20);

      session.removeSubscription(context.getKey());

      Assertions.assertEquals(1, messages.size(), "Expected exactly one message after single PUSH_DATA injection");
      validateSemtechJsonPayloadContainsInjectedRxpk(messages.getFirst(), pushJson);
    } finally {
      close(session);
    }
  }

  private JsonObject buildMinimalRxpkPushDataJson() {
    JsonObject root = new JsonObject();
    JsonArray rxpk = new JsonArray();

    JsonObject one = new JsonObject();
    one.addProperty("time", "2013-03-31T16:21:17.528002Z");
    one.addProperty("tmst", 3512348611L);
    one.addProperty("chan", 2);
    one.addProperty("rfch", 0);
    one.addProperty("freq", 866.349812);
    one.addProperty("stat", 1);
    one.addProperty("modu", "LORA");
    one.addProperty("datr", "SF7BW125");
    one.addProperty("codr", "4/6");
    one.addProperty("rssi", -35);
    one.addProperty("lsnr", 5.1);

    // payload: arbitrary bytes, base64-encoded.
    // size must match decoded byte length.
    String base64 = "VEVTVF9QQUNLRVRfMTIzNA=="; // "TEST_PACKET_1234"
    one.addProperty("size", 15);
    one.addProperty("data", base64);

    rxpk.add(one);
    root.add("rxpk", rxpk);

    return root;
  }

  private byte[] buildPushDataPacket(int token, byte[] gatewayEui, JsonObject json) {
    Assertions.assertNotNull(gatewayEui);
    Assertions.assertEquals(8, gatewayEui.length, "gatewayEui must be 8 bytes");

    byte[] jsonBytes = json.toString().getBytes(StandardCharsets.UTF_8);
    byte[] packet = new byte[4 + 8 + jsonBytes.length];

    packet[0] = (byte) (SEMTECH_PROTOCOL_VERSION & 0xFF);
    packet[1] = (byte) ((token >> 8) & 0xFF);
    packet[2] = (byte) (token & 0xFF);
    packet[3] = (byte) (PUSH_DATA & 0xFF);

    System.arraycopy(gatewayEui, 0, packet, 4, 8);
    System.arraycopy(jsonBytes, 0, packet, 12, jsonBytes.length);

    return packet;
  }

  private byte[] receiveUdp(DatagramSocket socket, int maxBytes) throws IOException {
    byte[] buffer = new byte[maxBytes];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    socket.receive(packet);

    byte[] data = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
    return data;
  }

  private void validatePushAck(byte[] ackBytes, int expectedToken) {
    Assertions.assertNotNull(ackBytes);
    Assertions.assertTrue(ackBytes.length >= 4, "PUSH_ACK must be at least 4 bytes");

    int version = ackBytes[0] & 0xFF;
    Assertions.assertEquals(SEMTECH_PROTOCOL_VERSION, version, "Semtech protocol version mismatch");

    int token = ((ackBytes[1] & 0xFF) << 8) | (ackBytes[2] & 0xFF);
    Assertions.assertEquals(expectedToken, token, "PUSH_ACK token mismatch");

    int identifier = ackBytes[3] & 0xFF;
    Assertions.assertEquals(PUSH_ACK, identifier, "Expected PUSH_ACK (0x01)");
  }

  private void validateSemtechJsonPayloadContainsInjectedRxpk(Message message, JsonObject injectedPushJson) {
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

    // This assumes your Semtech implementation publishes raw Semtech JSON or wraps it.
    // First try direct "rxpk" at top-level, then try common wrapper names.
    JsonObject semtechObject = object;
    if (!object.has("rxpk")) {
      if (object.has("semtech") && object.get("semtech").isJsonObject()) {
        semtechObject = object.getAsJsonObject("semtech");
      } else if (object.has("lora") && object.get("lora").isJsonObject()) {
        semtechObject = object.getAsJsonObject("lora");
      }
    }

    assertFieldPresent(semtechObject, "rxpk");
    Assertions.assertTrue(semtechObject.get("rxpk").isJsonArray(), "rxpk must be an array. Payload: " + payload);

    JsonArray rxpk = semtechObject.getAsJsonArray("rxpk");
    Assertions.assertTrue(rxpk.size() >= 1, "rxpk must contain at least one item. Payload: " + payload);

    JsonObject first = rxpk.get(0).getAsJsonObject();
    assertFieldPresent(first, "data");
    assertFieldPresent(first, "size");

    String injectedBase64 = injectedPushJson.getAsJsonArray("rxpk").get(0).getAsJsonObject().get("data").getAsString();
    int injectedSize = injectedPushJson.getAsJsonArray("rxpk").get(0).getAsJsonObject().get("size").getAsInt();

    Assertions.assertEquals(injectedBase64, first.get("data").getAsString(), "Injected base64 payload mismatch. Payload: " + payload);
    Assertions.assertEquals(injectedSize, first.get("size").getAsInt(), "Injected size mismatch. Payload: " + payload);
  }

  private void assertFieldPresent(JsonObject object, String fieldName) {
    Assertions.assertTrue(object.has(fieldName), "Missing field '" + fieldName + "'. Object: " + object);
    Assertions.assertFalse(object.get(fieldName).isJsonNull(), "Field '" + fieldName + "' must not be null. Object: " + object);
  }

  private byte[] hexToBytes(String hex) {
    int length = hex.length();
    Assertions.assertEquals(0, length % 2, "hex string length must be even");

    byte[] bytes = new byte[length / 2];
    for (int i = 0; i < length; i += 2) {
      int value = Integer.parseInt(hex.substring(i, i + 2), 16);
      bytes[i / 2] = (byte) value;
    }
    return bytes;
  }
}