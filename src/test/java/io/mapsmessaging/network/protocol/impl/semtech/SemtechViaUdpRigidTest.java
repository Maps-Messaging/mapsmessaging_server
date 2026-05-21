package io.mapsmessaging.network.protocol.impl.semtech;

import com.google.gson.*;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.semtech.json.PushDataJSON;
import io.mapsmessaging.network.protocol.impl.semtech.json.ReceivePacket;
import io.mapsmessaging.network.protocol.impl.semtech.json.StatPacket;
import io.mapsmessaging.network.protocol.impl.semtech.json.TxPackAck;
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

class SemtechViaUdpRigidTest extends BaseTestConfig {

  private static final String SUBSCRIBE_TOPIC = "/semtech/#";

  private static final String UDP_HOST = "127.0.0.1";
  private static final int UDP_PORT = 1700;

  private static final int VERSION = 0x02;

  private static final int PUSH_DATA = 0x00;
  private static final int PUSH_ACK = 0x01;

  private static final int PULL_DATA = 0x02;
  private static final int PULL_ACK = 0x04;

  private static final int TX_ACK = 0x05;

  private static final Gson GSON = new GsonBuilder().serializeNulls().create();

  @Test
  void testPushDataLoRaStrictRoundTrip() throws LoginException, IOException, InterruptedException {
    PushDataJSON expected = new PushDataJSON();
    expected.setRxpk(new ReceivePacket[] {
        buildExpectedReceivePacketLoRa(
            "2013-03-31T16:21:17.528002Z",
            3512348611L,
            866.349812,
            2,
            0,
            1,
            "LORA",
            "SF7BW125",
            "4/6",
            -35,
            5.1,
            15,
            "VEVTVF9QQUNLRVRfMTIzNA=="
        )
    });
    expected.setStat(null);

    PushDataJSON received = sendPushDataAndGetPublished(expected, buildIncomingSemtechLoRaJson());
    assertRxpkCount(received, 1);

    ReceivePacket rx = received.getRxpk()[0];
    assertReceivePacketLoRaEquals(expected.getRxpk()[0], rx);
  }

  @Test
  void testPushDataMultiRxpkStrict() throws LoginException, IOException, InterruptedException {
    ReceivePacket[] expectedRxpk = new ReceivePacket[] {
        buildExpectedReceivePacketLoRa(
            "2013-03-31T16:21:17.528002Z",
            1111111111L,
            866.349812,
            2,
            0,
            1,
            "LORA",
            "SF7BW125",
            "4/6",
            -35,
            5.1,
            4,
            "AQIDBA=="
        ),
        buildExpectedReceivePacketLoRa(
            "2013-03-31T16:21:18.528002Z",
            2222222222L,
            867.125000,
            3,
            1,
            1,
            "LORA",
            "SF12BW125",
            "4/5",
            -80,
            -1.25,
            5,
            "AQIDBAU="
        )
    };

    Message publishedMessage = sendPushDataAndGetPublishedMessage(buildIncomingSemtechMultiLoRaJson());
    JsonObject publishedJson = parsePublishedAsJsonObject(publishedMessage);

    assertRawSemtechRxpkEquals(publishedJson, expectedRxpk);
  }
  private Message sendPushDataAndGetPublishedMessage(JsonObject incomingSemtechJson)
      throws LoginException, IOException, InterruptedException {

    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch firstMessageLatch = new CountDownLatch(1);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      messages.add(messageEvent.getMessage());
      firstMessageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("semtechUdpRigidTest" + System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder =
          new SubscriptionContextBuilder(SUBSCRIBE_TOPIC, ClientAcknowledgement.AUTO);

      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);

      byte[] gatewayEui = hexToBytes("0102030405060708");
      int token = ThreadLocalRandom.current().nextInt(0, 0x10000);

      byte[] pushPacket = buildPushDataPacket(
          token,
          gatewayEui,
          incomingSemtechJson.toString().getBytes(StandardCharsets.UTF_8)
      );

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

      Assertions.assertEquals(1, messages.size(), "Expected exactly one published message for one PUSH_DATA");
      return messages.getFirst();
    } finally {
      close(session);
    }
  }

  private JsonObject parsePublishedAsJsonObject(Message message) {
    Assertions.assertNotNull(message, "Message must not be null");

    byte[] payloadBytes = message.getOpaqueData();
    Assertions.assertNotNull(payloadBytes, "Message payload must not be null");
    Assertions.assertTrue(payloadBytes.length > 0, "Message payload must not be empty");

    String payload = new String(payloadBytes, StandardCharsets.UTF_8).trim();
    Assertions.assertFalse(payload.isEmpty(), "Message payload must not be blank");

    try {
      JsonObject object = JsonParser.parseString(payload).getAsJsonObject();
      Assertions.assertTrue(object.isJsonObject(), "Payload must be a JSON object. Payload: " + payload);
      return object;
    } catch (Exception e) {
      Assertions.fail("Published payload is not valid JSON object. Payload: " + payload, e);
      return null;
    }
  }

  private void assertRawSemtechRxpkEquals(JsonObject published, ReceivePacket[] expected) {
    Assertions.assertNotNull(published, "Published JSON must not be null");
    Assertions.assertTrue(published.has("rxpk"), "Missing field 'rxpk'. Object: " + published);
    Assertions.assertTrue(published.get("rxpk").isJsonArray(), "rxpk must be an array. Object: " + published);

    JsonArray rxpk = published.getAsJsonArray("rxpk");
    Assertions.assertEquals(expected.length, rxpk.size(), "rxpk count mismatch. Object: " + published);

    for (int i = 0; i < expected.length; i++) {
      JsonObject actual = rxpk.get(i).getAsJsonObject();
      ReceivePacket exp = expected[i];

      assertJsonString(actual, "time", exp.getTime());
      assertJsonLong(actual, "tmst", exp.getTmst());
      assertJsonLong(actual, "chan", exp.getChan());
      assertJsonLong(actual, "rfch", exp.getRfch());
      assertJsonLong(actual, "stat", exp.getStat());
      assertJsonString(actual, "modu", exp.getModu());
      assertJsonString(actual, "codr", exp.getCodr());
      assertJsonLong(actual, "rssi", exp.getRssi());
      assertJsonLong(actual, "size", exp.getSize());
      assertJsonString(actual, "data", exp.getData());

      assertJsonDouble(actual, "freq", exp.getFreq(), 0.000001);
      assertJsonDouble(actual, "lsnr", exp.getLsnr(), 0.000001);

      assertJsonString(actual, "datr", exp.getDatr());
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
        "Field '" + field + "' mismatch. expected=" + expected + " actual=" + actual + " Object: " + object);
  }

  @Test
  void testPushDataFskNumericDatrStrict() throws LoginException, IOException, InterruptedException {
    PushDataJSON expected = new PushDataJSON();
    ReceivePacket fsk = new ReceivePacket();
    fsk.setTime("2013-03-31T16:21:17.528002Z");
    fsk.setTmst(3333333333L);
    fsk.setFreq(868.300000);
    fsk.setChan(0);
    fsk.setRfch(0);
    fsk.setStat(1);
    fsk.setModu("FSK");
    fsk.setDatr(""+50000L);     // numeric bitrate
    fsk.setCodr(null);
    fsk.setRssi(-42);
    fsk.setLsnr(0.0);
    fsk.setSize(3);
    fsk.setData("AQID");

    expected.setRxpk(new ReceivePacket[] { fsk });
    expected.setStat(null);

    PushDataJSON received = sendPushDataAndGetPublished(expected, buildIncomingSemtechFskJson());
    assertRxpkCount(received, 1);

    ReceivePacket rx = received.getRxpk()[0];
    Assertions.assertEquals("FSK", rx.getModu(), "modu mismatch");
    Assertions.assertEquals("50000", rx.getDatr(), "FSK datr numeric mismatch");
    Assertions.assertEquals(3L, rx.getSize(), "size mismatch");
    Assertions.assertEquals("AQID", rx.getData(), "data mismatch");
    assertDoubleEquals(868.300000, rx.getFreq(), 0.000001, "freq mismatch");
  }

  @Test
  void testPushDataCrcFailPreserved() throws LoginException, IOException, InterruptedException {
    PushDataJSON expected = new PushDataJSON();
    expected.setRxpk(new ReceivePacket[] {
        buildExpectedReceivePacketLoRa(
            "2013-03-31T16:21:17.528002Z",
            4444444444L,
            866.900000,
            1,
            0,
            -1,              // CRC fail
            "LORA",
            "SF9BW125",
            "4/7",
            -90,
            2.0,
            2,
            "AAE="
        )
    });

    PushDataJSON received = sendPushDataAndGetPublished(expected, buildIncomingSemtechCrcFailJson());
    assertRxpkCount(received, 1);

    ReceivePacket rx = received.getRxpk()[0];
    Assertions.assertEquals(-1L, rx.getStat(), "Expected stat=-1 to be preserved for CRC fail rxpk");
    Assertions.assertEquals("AAE=", rx.getData(), "data mismatch");
    Assertions.assertEquals(2L, rx.getSize(), "size mismatch");
  }

  @Test
  void testPullDataAckTokenMatches() throws IOException {
    int token = ThreadLocalRandom.current().nextInt(0, 0x10000);
    byte[] gatewayEui = hexToBytes("0102030405060708");

    byte[] pullData = buildPullDataPacket(token, gatewayEui);

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(2000);

      InetAddress address = InetAddress.getByName(UDP_HOST);
      DatagramPacket packet = new DatagramPacket(pullData, pullData.length, address, UDP_PORT);
      socket.send(packet);

      byte[] ackBytes = receiveUdp(socket, 64);
      validatePullAck(ackBytes, token);
    }
  }

  @Test
  void testTxAckAccepted() throws IOException {
    int token = ThreadLocalRandom.current().nextInt(0, 0x10000);
    byte[] gatewayEui = hexToBytes("0102030405060708");

    JsonObject txAckJson = new JsonObject();
    JsonObject inner = new JsonObject();
    inner.addProperty("error", "NONE");
    txAckJson.add("txpk_ack", inner);

    byte[] txAck = buildTxAckPacket(token, gatewayEui, txAckJson.toString().getBytes(StandardCharsets.UTF_8));

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(1500);

      InetAddress address = InetAddress.getByName(UDP_HOST);
      DatagramPacket packet = new DatagramPacket(txAck, txAck.length, address, UDP_PORT);
      socket.send(packet);

      // Protocol does not require an ACK for TX_ACK.
      // This test is simply "server doesn't throw / doesn't die / doesn't spam errors".
      // If you publish TX_ACK internally, wire in a subscription later and assert it.
    }
  }

  // -----------------------------
  // Core: send PUSH_DATA + capture published
  // -----------------------------

  private PushDataJSON sendPushDataAndGetPublished(PushDataJSON expectedPublished, JsonObject incomingSemtechJson)
      throws LoginException, IOException, InterruptedException {

    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch firstMessageLatch = new CountDownLatch(1);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      messages.add(messageEvent.getMessage());
      firstMessageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("semtechUdpRigidTest" + System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder =
          new SubscriptionContextBuilder(SUBSCRIBE_TOPIC, ClientAcknowledgement.AUTO);

      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);

      byte[] gatewayEui = hexToBytes("0102030405060708");
      int token = ThreadLocalRandom.current().nextInt(0, 0x10000);

      byte[] pushPacket = buildPushDataPacket(token, gatewayEui, incomingSemtechJson.toString().getBytes(StandardCharsets.UTF_8));

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

      Assertions.assertEquals(1, messages.size(), "Expected exactly one published message for one PUSH_DATA");

      PushDataJSON parsed = parsePublishedPushData(messages.getFirst());
      Assertions.assertNotNull(parsed, "Parsed PushDataJSON must not be null");

      // Basic strictness: presence of expected major section(s)
      if (expectedPublished.getRxpk() != null && expectedPublished.getRxpk().length > 0) {
        Assertions.assertNotNull(parsed.getRxpk(), "Expected rxpk to be present");
      }
      if (expectedPublished.getStat() != null) {
        Assertions.assertNotNull(parsed.getStat(), "Expected stats to be present");
      }

      return parsed;
    } finally {
      close(session);
    }
  }

  private PushDataJSON parsePublishedPushData(Message message) {
    Assertions.assertNotNull(message, "Message must not be null");

    byte[] payloadBytes = message.getOpaqueData();
    Assertions.assertNotNull(payloadBytes, "Message payload must not be null");
    Assertions.assertTrue(payloadBytes.length > 0, "Message payload must not be empty");

    String payload = new String(payloadBytes, StandardCharsets.UTF_8).trim();
    Assertions.assertFalse(payload.isEmpty(), "Message payload must not be blank");

    try {
      return GSON.fromJson(payload, PushDataJSON.class);
    } catch (Exception e) {
      Assertions.fail("Published payload is not compatible with PushDataJSON. Payload: " + payload, e);
      return null;
    }
  }

  // -----------------------------
  // Packet builders
  // -----------------------------

  private byte[] buildPushDataPacket(int token, byte[] gatewayEui, byte[] jsonBytes) {
    Assertions.assertNotNull(gatewayEui);
    Assertions.assertEquals(8, gatewayEui.length, "gatewayEui must be 8 bytes");

    byte[] packet = new byte[4 + 8 + jsonBytes.length];

    packet[0] = (byte) (VERSION & 0xFF);
    packet[1] = (byte) ((token >> 8) & 0xFF);
    packet[2] = (byte) (token & 0xFF);
    packet[3] = (byte) (PUSH_DATA & 0xFF);

    System.arraycopy(gatewayEui, 0, packet, 4, 8);
    System.arraycopy(jsonBytes, 0, packet, 12, jsonBytes.length);

    return packet;
  }

  private byte[] buildPullDataPacket(int token, byte[] gatewayEui) {
    Assertions.assertNotNull(gatewayEui);
    Assertions.assertEquals(8, gatewayEui.length, "gatewayEui must be 8 bytes");

    byte[] packet = new byte[4 + 8];
    packet[0] = (byte) (VERSION & 0xFF);
    packet[1] = (byte) ((token >> 8) & 0xFF);
    packet[2] = (byte) (token & 0xFF);
    packet[3] = (byte) (PULL_DATA & 0xFF);
    System.arraycopy(gatewayEui, 0, packet, 4, 8);

    return packet;
  }

  private byte[] buildTxAckPacket(int token, byte[] gatewayEui, byte[] jsonBytes) {
    Assertions.assertNotNull(gatewayEui);
    Assertions.assertEquals(8, gatewayEui.length, "gatewayEui must be 8 bytes");

    byte[] packet = new byte[4 + 8 + jsonBytes.length];
    packet[0] = (byte) (VERSION & 0xFF);
    packet[1] = (byte) ((token >> 8) & 0xFF);
    packet[2] = (byte) (token & 0xFF);
    packet[3] = (byte) (TX_ACK & 0xFF);

    System.arraycopy(gatewayEui, 0, packet, 4, 8);
    System.arraycopy(jsonBytes, 0, packet, 12, jsonBytes.length);

    return packet;
  }

  // -----------------------------
  // UDP helpers + ACK validation
  // -----------------------------

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
    Assertions.assertEquals(VERSION, version, "Semtech protocol version mismatch");

    int token = ((ackBytes[1] & 0xFF) << 8) | (ackBytes[2] & 0xFF);
    Assertions.assertEquals(expectedToken, token, "PUSH_ACK token mismatch");

    int identifier = ackBytes[3] & 0xFF;
    Assertions.assertEquals(PUSH_ACK, identifier, "Expected PUSH_ACK (0x01)");
  }

  private void validatePullAck(byte[] ackBytes, int expectedToken) {
    Assertions.assertNotNull(ackBytes);
    Assertions.assertTrue(ackBytes.length >= 4, "PULL_ACK must be at least 4 bytes");

    int version = ackBytes[0] & 0xFF;
    Assertions.assertEquals(VERSION, version, "Semtech protocol version mismatch");

    int token = ((ackBytes[1] & 0xFF) << 8) | (ackBytes[2] & 0xFF);
    Assertions.assertEquals(expectedToken, token, "PULL_ACK token mismatch");

    int identifier = ackBytes[3] & 0xFF;
    Assertions.assertEquals(PULL_ACK, identifier, "Expected PULL_ACK (0x04)");
  }

  // -----------------------------
  // Strict POJO assertions
  // -----------------------------

  private void assertRxpkCount(PushDataJSON received, int expectedCount) {
    Assertions.assertNotNull(received.getRxpk(), "rxpk must not be null");
    Assertions.assertEquals(expectedCount, received.getRxpk().length, "rxpk count mismatch");
  }

  private void assertReceivePacketLoRaEquals(ReceivePacket expected, ReceivePacket actual) {
    Assertions.assertNotNull(actual, "ReceivePacket must not be null");

    Assertions.assertEquals(expected.getTime(), actual.getTime(), "time mismatch");
    Assertions.assertEquals(expected.getTmst(), actual.getTmst(), "tmst mismatch");
    Assertions.assertEquals(expected.getChan(), actual.getChan(), "chan mismatch");
    Assertions.assertEquals(expected.getRfch(), actual.getRfch(), "rfch mismatch");
    Assertions.assertEquals(expected.getStat(), actual.getStat(), "stat mismatch");
    Assertions.assertEquals(expected.getModu(), actual.getModu(), "modu mismatch");
    Assertions.assertEquals(expected.getDatr(), actual.getDatr(), "datr_s mismatch");
    Assertions.assertEquals(expected.getCodr(), actual.getCodr(), "codr mismatch");
    Assertions.assertEquals(expected.getRssi(), actual.getRssi(), "rssi mismatch");
    Assertions.assertEquals(expected.getSize(), actual.getSize(), "size mismatch");
    Assertions.assertEquals(expected.getData(), actual.getData(), "data mismatch");

    assertDoubleEquals(expected.getFreq(), actual.getFreq(), 0.000001, "freq mismatch");
    assertDoubleEquals(expected.getLsnr(), actual.getLsnr(), 0.000001, "lsnr mismatch");
  }

  private void assertDoubleEquals(double expected, double actual, double delta, String message) {
    Assertions.assertTrue(Math.abs(expected - actual) <= delta, message + " expected=" + expected + " actual=" + actual);
  }

  private ReceivePacket buildExpectedReceivePacketLoRa(
      String time,
      long tmst,
      double freq,
      long chan,
      long rfch,
      long stat,
      String modu,
      String datr,
      String codr,
      long rssi,
      double lsnr,
      long size,
      String dataBase64) {

    ReceivePacket p = new ReceivePacket();
    p.setTime(time);
    p.setTmst(tmst);
    p.setFreq(freq);
    p.setChan(chan);
    p.setRfch(rfch);
    p.setStat(stat);
    p.setModu(modu);
    p.setDatr(datr);
    p.setCodr(codr);
    p.setRssi(rssi);
    p.setLsnr(lsnr);
    p.setSize(size);
    p.setData(dataBase64);
    return p;
  }

  // -----------------------------
  // Incoming Semtech JSON (raw-ish)
  // -----------------------------

  private JsonObject buildIncomingSemtechLoRaJson() {
    JsonObject root = new JsonObject();
    root.add("rxpk", GSON.fromJson("""
        [{
          "time":"2013-03-31T16:21:17.528002Z",
          "tmst":3512348611,
          "chan":2,
          "rfch":0,
          "freq":866.349812,
          "stat":1,
          "modu":"LORA",
          "datr":"SF7BW125",
          "codr":"4/6",
          "rssi":-35,
          "lsnr":5.1,
          "size":15,
          "data":"VEVTVF9QQUNLRVRfMTIzNA=="
        }]
        """, com.google.gson.JsonElement.class));
    return root;
  }

  private JsonObject buildIncomingSemtechMultiLoRaJson() {
    JsonObject root = new JsonObject();
    root.add("rxpk", GSON.fromJson("""
        [
          {
            "time":"2013-03-31T16:21:17.528002Z",
            "tmst":1111111111,
            "chan":2,
            "rfch":0,
            "freq":866.349812,
            "stat":1,
            "modu":"LORA",
            "datr":"SF7BW125",
            "codr":"4/6",
            "rssi":-35,
            "lsnr":5.1,
            "size":4,
            "data":"AQIDBA=="
          },
          {
            "time":"2013-03-31T16:21:18.528002Z",
            "tmst":2222222222,
            "chan":3,
            "rfch":1,
            "freq":867.125,
            "stat":1,
            "modu":"LORA",
            "datr":"SF12BW125",
            "codr":"4/5",
            "rssi":-80,
            "lsnr":-1.25,
            "size":5,
            "data":"AQIDBAU="
          }
        ]
        """, com.google.gson.JsonElement.class));
    return root;
  }

  private JsonObject buildIncomingSemtechStatOnlyJson() {
    return GSON.fromJson("""
        {
          "stat": {
            "time":"2013-03-31T16:21:17.528002Z",
            "lati":-33.1234,
            "longitude":151.1234,
            "alti":12,
            "rxnb":10,
            "rxok":9,
            "rxfw":7,
            "ackr":100.0,
            "dwnb":2,
            "txnb":1
          }
        }
        """, JsonObject.class);
  }

  private JsonObject buildIncomingSemtechFskJson() {
    return GSON.fromJson("""
        {
          "rxpk": [{
            "time":"2013-03-31T16:21:17.528002Z",
            "tmst":3333333333,
            "chan":0,
            "rfch":0,
            "freq":868.3,
            "stat":1,
            "modu":"FSK",
            "datr":50000,
            "rssi":-42,
            "lsnr":0.0,
            "size":3,
            "data":"AQID"
          }]
        }
        """, JsonObject.class);
  }

  private JsonObject buildIncomingSemtechCrcFailJson() {
    return GSON.fromJson("""
        {
          "rxpk": [{
            "time":"2013-03-31T16:21:17.528002Z",
            "tmst":4444444444,
            "chan":1,
            "rfch":0,
            "freq":866.9,
            "stat":-1,
            "modu":"LORA",
            "datr":"SF9BW125",
            "codr":"4/7",
            "rssi":-90,
            "lsnr":2.0,
            "size":2,
            "data":"AAE="
          }]
        }
        """, JsonObject.class);
  }

  // -----------------------------
  // misc
  // -----------------------------

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