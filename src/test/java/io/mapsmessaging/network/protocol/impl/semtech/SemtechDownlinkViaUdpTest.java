package io.mapsmessaging.network.protocol.impl.semtech;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class SemtechDownlinkViaUdpTest extends BaseTestConfig {

  private static final String UDP_HOST = "127.0.0.1";
  private static final int UDP_PORT = 1700;

  private static final String OUTBOUND_ROOT = "/semtech/outbound";

  private static final int VERSION = 0x02;

  private static final int PULL_DATA = 0x02;
  private static final int PULL_ACK = 0x04;

  private static final int PULL_RESP = 0x03;

  private static final int TX_ACK = 0x05;

  @Test
  void testDownlinkRegisteredGatewayReceivesPullRespAndTxAckIsAccepted()
      throws LoginException, IOException, InterruptedException, ExecutionException, TimeoutException {

    String gatewayId = "0102030405060708";
    byte[] gatewayEui = hexToBytes(gatewayId);

    try (DatagramSocket gatewaySocket = new DatagramSocket()) {
      gatewaySocket.setSoTimeout(3000);

      registerGatewayWithPullData(gatewaySocket, gatewayEui);

      JsonObject expectedTxpk = buildTxpk();
      JsonObject pullRespJson = new JsonObject();
      pullRespJson.add("txpk", expectedTxpk);

      publishDownlinkRequestToMaps(gatewayId, pullRespJson.toString());
      // 3) Second pull triggers the server to actually send queued downlink
      registerGatewayWithPullData(gatewaySocket, gatewayEui);

      // 4) Now we should receive PULL_RESP
      byte[] pullRespFrame = receiveUdp(gatewaySocket, 4096);


      SemtechFrame parsed = parseSemtechFrame(pullRespFrame);
      Assertions.assertEquals(PULL_RESP, parsed.identifier, "Expected PULL_RESP (0x03)");

      JsonObject actualJson = parseJsonObject(new String(parsed.jsonBytes, StandardCharsets.UTF_8));

      Assertions.assertTrue(actualJson.has("txpk"), "PULL_RESP JSON must contain txpk. JSON: " + actualJson);
      Assertions.assertTrue(actualJson.get("txpk").isJsonObject(), "txpk must be an object. JSON: " + actualJson);

      JsonObject actualTxpk = actualJson.getAsJsonObject("txpk");
      assertTxpkEquals(expectedTxpk, actualTxpk);

      JsonObject txAckJson = new JsonObject();
      JsonObject txpkAck = new JsonObject();
      txpkAck.addProperty("error", "NONE");
      txAckJson.add("txpk_ack", txpkAck);

      InetAddress address = InetAddress.getByName(UDP_HOST);
      byte[] txAckFrame = buildTxAckFrame(parsed.token, gatewayEui, txAckJson.toString().getBytes(StandardCharsets.UTF_8));
      DatagramPacket txAckPacket = new DatagramPacket(txAckFrame, txAckFrame.length, address, UDP_PORT);
      gatewaySocket.send(txAckPacket);
    }
  }

  @Test
  void testDownlinkUnregisteredGatewayDoesNotSendAnyUdp()
      throws LoginException, IOException, InterruptedException, ExecutionException, TimeoutException {

    String registeredGatewayId = "0102030405060708";
    byte[] registeredGatewayEui = hexToBytes(registeredGatewayId);

    String unregisteredGatewayId = "1112131415161718";

    try (DatagramSocket gatewaySocket = new DatagramSocket()) {
      gatewaySocket.setSoTimeout(1500);

      // Register ONLY the first gateway (learn address + mark known)
      registerGatewayWithPullData(gatewaySocket, registeredGatewayEui);

      // Publish downlink to an unregistered gateway topic
      JsonObject expectedTxpk = buildTxpk();
      JsonObject pullRespJson = new JsonObject();
      pullRespJson.add("txpk", expectedTxpk);

      publishDownlinkRequestToMaps(unregisteredGatewayId, pullRespJson.toString());

      // We must NOT receive a PULL_RESP on the registered gateway socket,
      // because the outbound target gateway is unknown/unregistered.
      Assertions.assertThrows(SocketTimeoutException.class, () -> receiveUdp(gatewaySocket, 4096),
          "Expected no UDP downlink for unregistered gatewayId=" + unregisteredGatewayId);
    }
  }

  private void registerGatewayWithPullData(DatagramSocket gatewaySocket, byte[] gatewayEui) throws IOException {
    int pullToken = ThreadLocalRandom.current().nextInt(0, 0x10000);
    byte[] pullDataFrame = buildPullDataFrame(pullToken, gatewayEui);

    InetAddress address = InetAddress.getByName(UDP_HOST);
    DatagramPacket pullDataPacket = new DatagramPacket(pullDataFrame, pullDataFrame.length, address, UDP_PORT);
    gatewaySocket.send(pullDataPacket);

    byte[] pullAck = receiveUdp(gatewaySocket, 64);
    assertHeader(pullAck, PULL_ACK, pullToken);
  }

  private void publishDownlinkRequestToMaps(String gatewayId, String jsonPayload)
      throws LoginException, IOException, ExecutionException, InterruptedException, TimeoutException {

    String topic = OUTBOUND_ROOT + "/" + gatewayId;

    Session session = createSession("semtechDownlinkPublisher" + System.nanoTime(), 60, 60, false, null);
    Assertions.assertNotNull(session);

    try {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(jsonPayload.getBytes(StandardCharsets.UTF_8));
      Message message = messageBuilder.build();

      session.findDestination(topic, DestinationType.TOPIC)
          .thenApply(destination -> {
            try {
              destination.storeMessage(message);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            return destination;
          })
          .get(1, TimeUnit.SECONDS);

    } finally {
      close(session);
    }
  }

  private JsonObject buildTxpk() {
    JsonObject txpk = new JsonObject();
    txpk.addProperty("imme", true);
    txpk.addProperty("freq", 866.349812);
    txpk.addProperty("rfch", 0);
    txpk.addProperty("powe", 14);
    txpk.addProperty("modu", "LORA");
    txpk.addProperty("datr", "SF7BW125");
    txpk.addProperty("codr", "4/5");
    txpk.addProperty("ipol", true);
    txpk.addProperty("size", 4);
    txpk.addProperty("data", "AQIDBA==");
    return txpk;
  }

  private void assertTxpkEquals(JsonObject expected, JsonObject actual) {
    assertJsonBoolean(actual, "imme", expected.get("imme").getAsBoolean());
    assertJsonDouble(actual, "freq", expected.get("freq").getAsDouble(), 0.000001);
    assertJsonLong(actual, "rfch", expected.get("rfch").getAsLong());
    assertJsonLong(actual, "powe", expected.get("powe").getAsLong());
    assertJsonString(actual, "modu", expected.get("modu").getAsString());
    assertJsonString(actual, "datr", expected.get("datr").getAsString());
    assertJsonString(actual, "codr", expected.get("codr").getAsString());
    assertJsonBoolean(actual, "ipol", expected.get("ipol").getAsBoolean());
    assertJsonLong(actual, "size", expected.get("size").getAsLong());
    assertJsonString(actual, "data", expected.get("data").getAsString());
  }

  private byte[] buildPullDataFrame(int token, byte[] gatewayEui) {
    Assertions.assertNotNull(gatewayEui);
    Assertions.assertEquals(8, gatewayEui.length, "gatewayEui must be 8 bytes");

    byte[] frame = new byte[4 + 8];
    frame[0] = (byte) (VERSION & 0xFF);
    frame[1] = (byte) ((token >> 8) & 0xFF);
    frame[2] = (byte) (token & 0xFF);
    frame[3] = (byte) (PULL_DATA & 0xFF);
    System.arraycopy(gatewayEui, 0, frame, 4, 8);
    return frame;
  }

  private byte[] buildTxAckFrame(int token, byte[] gatewayEui, byte[] jsonBytes) {
    Assertions.assertNotNull(gatewayEui);
    Assertions.assertEquals(8, gatewayEui.length, "gatewayEui must be 8 bytes");

    byte[] frame = new byte[4 + 8 + jsonBytes.length];
    frame[0] = (byte) (VERSION & 0xFF);
    frame[1] = (byte) ((token >> 8) & 0xFF);
    frame[2] = (byte) (token & 0xFF);
    frame[3] = (byte) (TX_ACK & 0xFF);
    System.arraycopy(gatewayEui, 0, frame, 4, 8);
    System.arraycopy(jsonBytes, 0, frame, 12, jsonBytes.length);
    return frame;
  }

  private byte[] receiveUdp(DatagramSocket socket, int maxBytes) throws IOException {
    byte[] buffer = new byte[maxBytes];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    socket.receive(packet);

    byte[] data = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
    return data;
  }

  private void assertHeader(byte[] frame, int expectedIdentifier, int expectedToken) {
    SemtechFrame parsed = parseSemtechFrame(frame);
    Assertions.assertEquals(expectedIdentifier, parsed.identifier, "Identifier mismatch");
    Assertions.assertEquals(expectedToken, parsed.token, "Token mismatch");
  }

  private SemtechFrame parseSemtechFrame(byte[] frame) {
    Assertions.assertNotNull(frame);
    Assertions.assertTrue(frame.length >= 4, "Frame must be at least 4 bytes");

    int version = frame[0] & 0xFF;
    Assertions.assertEquals(VERSION, version, "Semtech version mismatch");

    int token = ((frame[1] & 0xFF) << 8) | (frame[2] & 0xFF);
    int identifier = frame[3] & 0xFF;

    byte[] jsonBytes = new byte[Math.max(0, frame.length - 4)];
    if (jsonBytes.length > 0) {
      System.arraycopy(frame, 4, jsonBytes, 0, jsonBytes.length);
    }

    SemtechFrame parsed = new SemtechFrame();
    parsed.token = token;
    parsed.identifier = identifier;
    parsed.jsonBytes = jsonBytes;
    return parsed;
  }

  private JsonObject parseJsonObject(String json) {
    try {
      JsonElement element = JsonParser.parseString(json);
      Assertions.assertTrue(element.isJsonObject(), "Expected JSON object. JSON: " + json);
      return element.getAsJsonObject();
    } catch (Exception e) {
      Assertions.fail("Invalid JSON received: " + json, e);
      return null;
    }
  }

  private void assertJsonString(JsonObject object, String field, String expected) {
    Assertions.assertTrue(object.has(field), "Missing field '" + field + "'. Object: " + object);
    Assertions.assertFalse(object.get(field).isJsonNull(), "Field '" + field + "' must not be null. Object: " + object);
    Assertions.assertEquals(expected, object.get(field).getAsString(), "Field '" + field + "' mismatch. Object: " + object);
  }

  private void assertJsonLong(JsonObject object, String field, long expected) {
    Assertions.assertTrue(object.has(field), "Missing field '" + field + "'. Object: " + object);
    Assertions.assertFalse(object.get(field).isJsonNull(), "Field '" + field + "' must not be null. Object: " + object);
    Assertions.assertEquals(expected, object.get(field).getAsLong(), "Field '" + field + "' mismatch. Object: " + object);
  }

  private void assertJsonDouble(JsonObject object, String field, double expected, double delta) {
    Assertions.assertTrue(object.has(field), "Missing field '" + field + "'. Object: " + object);
    Assertions.assertFalse(object.get(field).isJsonNull(), "Field '" + field + "' must not be null. Object: " + object);
    double actual = object.get(field).getAsDouble();
    Assertions.assertTrue(Math.abs(expected - actual) <= delta,
        "Field '" + field + "' mismatch expected=" + expected + " actual=" + actual + " Object: " + object);
  }

  private void assertJsonBoolean(JsonObject object, String field, boolean expected) {
    Assertions.assertTrue(object.has(field), "Missing field '" + field + "'. Object: " + object);
    Assertions.assertFalse(object.get(field).isJsonNull(), "Field '" + field + "' must not be null. Object: " + object);
    Assertions.assertEquals(expected, object.get(field).getAsBoolean(), "Field '" + field + "' mismatch. Object: " + object);
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

  private static class SemtechFrame {
    private int token;
    private int identifier;
    private byte[] jsonBytes;
  }
}