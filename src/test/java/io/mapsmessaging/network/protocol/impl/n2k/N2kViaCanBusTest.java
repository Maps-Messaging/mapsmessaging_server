package io.mapsmessaging.network.protocol.impl.n2k;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class N2kViaCanBusTest extends BaseTestConfig {

  private static final Pattern CANDUMP_PATTERN =
      Pattern.compile("^\\(\\d+\\.\\d+\\)\\s+(\\S+)\\s+([0-9A-Fa-f]+)#([0-9A-Fa-f]*)\\s*$");

  private static final String[] CANDUMP_LINES = new String[]{
      "(1745600961.335462) can0 11FC1063#003DFFFF02000100",
      "(1745600961.335462) can0 11FC1063#0101000C0153514C",
      "(1745600961.336482) can0 11FC1063#0220536572766572",
      "(1745600961.336482) can0 11FC1063#0310FBCF24EB4EF3",
      "(1745600961.336482) can0 11FC1063#0402004500000000",
      "(1745600961.336482) can0 11FC1063#0504000002000501",
      "(1745600961.336482) can0 11FC1063#0644423210FBCF24",
      "(1745600961.336482) can0 11FC1063#07EB4EFF20006000",
      "(1745600961.336482) can0 11FC1063#080000F33F0000FF",
      "(1745600961.336482) can0 11FC1163#002BFFFF02001600",
      "(1745600961.336482) can0 11FC1163#01FFFF01000D0148",
      "(1745600961.336482) can0 11FC1163#02657265546F5468",
      "(1745600961.336482) can0 11FC1163#0365726527020010",
      "(1745600961.336482) can0 11FC1163#0401416E64204261",
      "(1745600961.336482) can0 11FC1163#05636B2041676169",
      "(1745600961.337436) can0 11FC1163#066E1BFFFFFFFFFF",
      "(1745600961.337436) can0 11FC1263#001DFFFF02000B01",
      "(1745600961.337436) can0 11FC1263#0142656E746C6569",
      "(1745600961.337436) can0 11FC1263#02676810FBCF24EB",
      "(1745600961.337436) can0 11FC1263#034E02180003F01E",
      "(1745600961.337436) can0 11FC1263#0400FFFFFFFFFFFF",
      "(1745600961.337436) can0 11FC1363#0039FFFF02006400",
      "(1745600961.337436) can0 11FC1363#01FFFF020001000B",
      "(1745600961.337436) can0 11FC1363#020142656E746C65",
      "(1745600961.337436) can0 11FC1363#0369676800A01CE9",
      "(1745600961.337436) can0 11FC1363#0440F32056210010",
      "(1745600961.337436) can0 11FC1363#0501456173742042",
      "(1745600961.337436) can0 11FC1363#06656E746C656967",
      "(1745600961.337436) can0 11FC1363#076800F141EB8047",
      "(1745600961.337436) can0 11FC1363#08AA56FFFFFFFFFF",
      "(1745600961.337436) can0 11FC1463#003BFFFF03006400",
      "(1745600961.337436) can0 11FC1463#01FFFF020001000B",
      "(1745600961.337436) can0 11FC1463#020142656E746C65",
      "(1745600961.337436) can0 11FC1463#0369676821001001",
      "(1745600961.337436) can0 11FC1463#0445617374204265",
      "(1745600961.337436) can0 11FC1463#056E746C65696768",
      "(1745600961.337436) can0 11FC1463#0663001001466177",
      "(1745600961.337436) can0 11FC1463#076B6E6572204265",
      "(1745600961.337436) can0 11FC1463#0861636F6EFFFFFF",
      "(1745600961.337436) can0 11FC1563#0014FFFF02006400",
      "(1745600961.337436) can0 11FC1563#01FFFF020001001E",
      "(1745600961.337436) can0 11FC1563#0200FC02001400FD",
      "(1745600961.337436) can0 11FC1663#0035FFFF02006400",
      "(1745600961.337436) can0 11FC1663#01FFFF0200010013",
      "(1745600961.337436) can0 11FC1663#020142656E746C65",
      "(1745600961.337436) can0 11FC1663#036967682D436F6D",
      "(1745600961.337436) can0 11FC1663#046D656E74020014",
      "(1745600961.337436) can0 11FC1663#0501476C656E6875",
      "(1745600961.337436) can0 11FC1663#066E746C792D436F",
      "(1745600961.338397) can0 11FC1663#076D6D656E74FFFF",
      "(1745600961.338397) can0 11FC1763#003DFFFF02000F00",
      "(1745600961.338397) can0 11FC1763#01FFFF0200170148",
      "(1745600961.338397) can0 11FC1763#0265726520746F20",
      "(1745600961.338397) can0 11FC1763#0354686572652D43",
      "(1745600961.338397) can0 11FC1763#046F6D6D656E7405",
      "(1745600961.338397) can0 11FC1763#05001A01416E6420",
      "(1745600961.338397) can0 11FC1763#064261636B204167",
      "(1745600961.338397) can0 11FC1763#0761696E202D2043",
      "(1745600961.338397) can0 11FC1763#086F6D6D656E74FF",
      "(1745600961.338397) can0 11FC1863#002FFFFF02000300",
      "(1745600961.338397) can0 11FC1863#010100160153514C",
      "(1745600961.338397) can0 11FC1863#0220536572766572",
      "(1745600961.338397) can0 11FC1863#03202D20436F6D6D",
      "(1745600961.338397) can0 11FC1863#04656E7402000F01",
      "(1745600961.338397) can0 11FC1863#05444232202D2043",
      "(1745600961.338397) can0 11FC1863#066F6D6D656E74FF",
      "(1745600961.338397) can0 11FC1A63#0030000002000200",
      "(1745600961.338397) can0 11FC1A63#010100FFFF6C000A",
      "(1745600961.338397) can0 11FC1A63#02014D634B696E6E",
      "(1745600961.338397) can0 11FC1A63#036F6E404E08ECC0",
      "(1745600961.338397) can0 11FC1A63#049649505C000801",
      "(1745600961.338397) can0 11FC1A63#054F726D6F6E64A0",
      "(1745600961.338397) can0 11FC1A63#06311FEC607E2650",
      "(1745600961.338397) can0 11F10163#0023A012AB0F00F8",
      "(1745600961.338397) can0 11F10163#0140C2931EFFEB4E",
      "(1745600961.338397) can0 11F10163#0210FBCF243D8533",
      "(1745600961.338397) can0 11F10163#03E9285F4056FC5B",
      "(1745600961.338397) can0 11F10163#043D33009218D439",
      "(1745600961.338397) can0 11F10163#05F8FFFFFFFFFFFF",
      "(1745600961.338397) can0 11F90463#0022FFA00F000044",
      "(1745600961.338397) can0 11F90463#01109D1A29EB4EDB",
      "(1745600961.338397) can0 11F90463#0253385500000000",
      "(1745600961.338397) can0 11F90463#03010000000060E3",
      "(1745600961.338397) can0 11F90463#041600CA91FE0404",
      "(1745600961.338397) can0 11F90563#0040FFFF02000100",
      "(1745600961.338397) can0 11F90563#010200E00801526F",
      "(1745600961.338397) can0 11F90563#0275746531FF0100",
      "(1745600961.338397) can0 11F90563#030D01576179706F",
      "(1745600961.338397) can0 11F90563#04696E744F6E6500",
      "(1745600961.338397) can0 11F90563#0560E31600CA91FE",
      "(1745600961.338397) can0 11F90563#0602000D01576179",
      "(1745600961.338397) can0 11F90563#07706F696E745477",
      "(1745600961.338397) can0 11F90563#086F0069201700C1",
      "(1745600961.338397) can0 11F90563#0954FEFFFFFFFFFF",
      "(1745600961.338397) can0 11FB1063#004E70FF17324C27",
      "(1745600961.338397) can0 11FB1063#011E6D6439303030",
      "(1745600961.338397) can0 11FB1063#0236393930303036",
      "(1745600961.338397) can0 11FB1063#03390B0131383030",
      "(1745600961.338397) can0 11FB1063#042048656C70C066",
      "(1745600961.338397) can0 11FB1063#054AE9802CF35510",
      "(1745600961.338397) can0 11FB1063#06FBCF2417324C27",
      "(1745600961.338397) can0 11FB1063#071E7FFD39303030",
      "(1745600961.338397) can0 11FB1063#0837303930303037",
      "(1745600961.338397) can0 11FB1063#093010FBCF24EB4E",
      "(1745600961.338397) can0 11FB1063#0AFFFF680601496F",
      "(1745600961.338397) can0 11FB1063#0B6E61FFFFFFFFFF"
  };

  @Test
  void testSimpleSend() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch firstMessageLatch = new CountDownLatch(1);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      firstMessageLatch.countDown();
      messages.add(messageEvent.getMessage());
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("n2kSimpleTest"+System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder =
          new SubscriptionContextBuilder("/vcan0/#", ClientAcknowledgement.AUTO);

      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);
      injectCandumpFramesIntoVcan("vcan0", CANDUMP_LINES);

      boolean received = firstMessageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "No N2K messages were published to /vcan0/# after CAN injection");
      Assertions.assertTrue(receivedCount.get() > 0, "Expected at least one message after injection");

      int attempts = 0;
      do {
        delay(100);
        attempts++;
      } while (CANDUMP_LINES.length != messages.size() && attempts < 20);
      session.removeSubscription(context.getKey());

      // 6 invalid messages
      Assertions.assertEquals(CANDUMP_LINES.length-6, messages.size(), "Expected message count to match injected CAN frames");

      for (Message message : messages) {
        validatePayload(message);
      }
    } finally {
      close(session);
    }
  }

  @Test
  void testRandomNonN2kFramesAreHandled() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch lastMessageLatch = new CountDownLatch(3);
    List<Message> messages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      messages.add(messageEvent.getMessage());
      lastMessageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("n2kRandomFramesTest"+System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder("/vcan0/#", ClientAcknowledgement.AUTO);
      SubscriptionContext context = subscriptionContextBuilder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      SubscribedEventManager manager = session.addSubscription(context);

      int sourceAddress = 99;
      int destinationAddress = 255;
      int priority = 4;

      int unknownPgnA = 0x01FF10;
      int unknownPgnB = 0x00EA99;
      int unknownPgnC = 0x00ABCD;

      int canIdA = buildJ1939CanId(priority, unknownPgnA, destinationAddress, sourceAddress);
      int canIdB = buildJ1939CanId(priority, unknownPgnB, destinationAddress, sourceAddress);
      int canIdC = buildJ1939CanId(priority, unknownPgnC, destinationAddress, sourceAddress);
      byte[] expectedA = hexToBytes("DEADBEEFDEADBEEF");
      byte[] expectedB = hexToBytes("0001020304050607");
      byte[] expectedC = hexToBytes("FFFFFFFFFFFFFFFF");

      delay(100);
      injectRawCanFramesIntoVcan("vcan0", new RawFrame[]{
          new RawFrame(canIdA, true, "DEADBEEFDEADBEEF"),
          new RawFrame(canIdB, true, "0001020304050607"),
          new RawFrame(canIdC, true, "FFFFFFFFFFFFFFFF")
      });
      boolean received = lastMessageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "No messages were published after injecting random frames");
      Assertions.assertEquals(3, receivedCount.get(), "Expected one published message per injected CAN frame");
      session.removeSubscription(context.getKey());

      int attempts = 0;
      do {
        delay(100);
        attempts++;
      } while (messages.size() != 3 && attempts < 20);

      Assertions.assertEquals(3, messages.size(), "Expected three messages in total");

      validateUnknownPayload(messages.get(0),  expectedA);
      validateUnknownPayload(messages.get(1),   expectedB);
      validateUnknownPayload(messages.get(2),   expectedC);

      List<Message> after = new CopyOnWriteArrayList<>();
      CountDownLatch validLatch = new CountDownLatch(1);

      MessageListener validListener = messageEvent -> {
        after.add(messageEvent.getMessage());
        validLatch.countDown();
        messageEvent.getCompletionTask().run();
      };

      close(session);

      Session session2 = createSession("n2kRandomFramesTestPhase2", 60, 60, false, validListener);
      try {
        SubscriptionContextBuilder builder2 =
            new SubscriptionContextBuilder("/vcan0/#", ClientAcknowledgement.AUTO);

        SubscriptionContext context2 = builder2
            .setQos(QualityOfService.AT_MOST_ONCE)
            .setReceiveMaximum(100)
            .build();

        session2.addSubscription(context2);

        injectCandumpFramesIntoVcan("vcan0", new String[]{CANDUMP_LINES[0]});

        boolean ok = validLatch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(ok, "Server did not continue to process valid frames after random frame injection");
        Assertions.assertEquals(1, after.size(), "Expected exactly one message after valid injection");
        validatePayload(after.get(0));
      } finally {
        close(session2);
      }
    } finally {
      close(session);
    }
  }

  private void validateUnknownPayload(Message message, byte[] expectedPayloadBytes) {
    Assertions.assertNotNull(message, "Message must not be null");

    byte[] payloadBytes = message.getOpaqueData();
    Assertions.assertNotNull(payloadBytes, "Message payload must not be null");
    Assertions.assertTrue(payloadBytes.length > 0, "Message payload must not be empty");

    String payload = new String(payloadBytes, StandardCharsets.UTF_8).trim();
    Assertions.assertFalse(payload.isEmpty(), "Message payload must not be blank");

    JsonElement root = JsonParser.parseString(payload);
    Assertions.assertTrue(root.isJsonObject(), "Payload must be a JSON object. Payload: " + payload);
    JsonObject object = root.getAsJsonObject();

    assertFieldPresent(object, "canId");
    assertFieldPresent(object, "dlc");
    assertFieldPresent(object, "extended");
    assertFieldPresent(object, "data");

    int dlc = object.get("dlc").getAsInt();
    Assertions.assertTrue(dlc >= 0 && dlc <= 8, "dlc must be in [0..8]. Payload: " + payload);

    String data = object.get("data").getAsString();
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(data);
    } catch (IllegalArgumentException e) {
      Assertions.fail("data is not valid Base64. Payload: " + payload, e);
      return;
    }
    Assertions.assertEquals(dlc, decoded.length, "Decoded data length must match dlc. Payload: " + payload);

    Assertions.assertFalse(object.has("j1939"), "Unknown payload must not include j1939. Payload: " + payload);
    Assertions.assertFalse(object.has("n2k"), "Unknown payload must not include n2k. Payload: " + payload);
    Assertions.assertArrayEquals(
        expectedPayloadBytes,
        decoded,
        "Decoded CAN payload does not match injected payload. Payload: " + payload
    );

  }

  private void validatePayload(Message message) {
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

    assertFieldPresent(object, "canId");
    assertFieldPresent(object, "dlc");
    assertFieldPresent(object, "extended");
    assertFieldPresent(object, "data");

    long canId = object.get("canId").getAsLong();
    int dlc = object.get("dlc").getAsInt();
    boolean extended = object.get("extended").getAsBoolean();
    String data = object.get("data").getAsString();

    Assertions.assertTrue(canId >= 0, "canId must be >= 0. Payload: " + payload);
    Assertions.assertTrue(dlc >= 0 && dlc <= 8, "dlc must be in [0..8]. dlc=" + dlc + " Payload: " + payload);
    Assertions.assertFalse(data.isBlank(), "data must not be blank. Payload: " + payload);

    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(data);
    } catch (IllegalArgumentException e) {
      Assertions.fail("data is not valid Base64. data=" + data + " Payload: " + payload, e);
      return;
    }

    Assertions.assertEquals(dlc, decoded.length, "Base64 decoded data length must match dlc. dlc=" + dlc
        + " decoded=" + decoded.length + " Payload: " + payload);

    boolean inferredExtended = canId > 0x7FF;
    Assertions.assertEquals(inferredExtended, extended, "extended flag mismatch. canId=" + canId
        + " inferredExtended=" + inferredExtended + " Payload: " + payload);

    if (object.has("j1939") && !object.get("j1939").isJsonNull()) {
      Assertions.assertTrue(object.get("j1939").isJsonObject(), "j1939 must be an object if present. Payload: " + payload);
      JsonObject j1939 = object.getAsJsonObject("j1939");

      assertFieldPresent(j1939, "pgn");
      int pgn = j1939.get("pgn").getAsInt();
      Assertions.assertTrue(pgn >= 0 && pgn <= 262143, "pgn must be in [0..262143]. pgn=" + pgn + " Payload: " + payload);
    }
  }

  private int buildJ1939CanId(int priority, int pgn, int destination, int source) {
    Assertions.assertTrue(priority >= 0 && priority <= 7, "priority must be [0..7]");
    Assertions.assertTrue(pgn >= 0 && pgn <= 262143, "pgn must be [0..262143]");
    Assertions.assertTrue(destination >= 0 && destination <= 255, "destination must be [0..255]");
    Assertions.assertTrue(source >= 0 && source <= 255, "source must be [0..255]");

    int dp = (pgn >> 16) & 0x01;
    int pf = (pgn >> 8) & 0xFF;
    int ps;

    if (pf < 240) {
      ps = destination & 0xFF;
    } else {
      ps = pgn & 0xFF;
    }

    int canId = 0;
    canId |= (priority & 0x7) << 26;
    canId |= (dp & 0x1) << 24;
    canId |= (pf & 0xFF) << 16;
    canId |= (ps & 0xFF) << 8;
    canId |= (source & 0xFF);

    return canId;
  }

  private void assertFieldPresent(JsonObject object, String fieldName) {
    Assertions.assertTrue(object.has(fieldName), "Missing field '" + fieldName + "'. Object: " + object);
    Assertions.assertFalse(object.get(fieldName).isJsonNull(), "Field '" + fieldName + "' must not be null. Object: " + object);
  }


  private void injectRawCanFramesIntoVcan(String interfaceName, RawFrame[] frames) throws IOException {
    try (SocketCanDevice socketCanDevice = new SocketCanDevice(interfaceName)) {
      for (RawFrame rawFrame : frames) {
        byte[] payload = hexToBytes(rawFrame.dataHex);
        Assertions.assertTrue(payload.length <= 8, "Raw frame payload must be <= 8 bytes");

        CanFrame frame = new CanFrame(rawFrame.canId, rawFrame.extended, payload.length, payload);
        socketCanDevice.writeFrame(frame);
        delay(50);
      }
    }
    catch(Throwable t) {
      t.printStackTrace();
    }
  }


  private void injectCandumpFramesIntoVcan(String interfaceName, String[] candumpLines) throws IOException {
    try (SocketCanDevice socketCanDevice = new SocketCanDevice(interfaceName)) {
      for (String line : candumpLines) {
        ParsedFrame parsedFrame = parseCandumpLine(line);
        if (parsedFrame == null) {
          continue;
        }

        boolean extendedFrame = parsedFrame.canId > 0x7FF;
        CanFrame frame = new CanFrame(parsedFrame.canId, extendedFrame, parsedFrame.dataLength, parsedFrame.data);
        socketCanDevice.writeFrame(frame);
        delay(10);
      }
    }
  }

  private ParsedFrame parseCandumpLine(String line) {
    Matcher matcher = CANDUMP_PATTERN.matcher(line);
    if (!matcher.matches()) {
      return null;
    }

    String canIdHex = matcher.group(2);
    String dataHex = matcher.group(3);

    if (dataHex.length() % 2 != 0) {
      throw new IllegalArgumentException("Odd-length CAN data field: " + line);
    }

    int dataLength = dataHex.length() / 2;
    if (dataLength > 8) {
      throw new IllegalArgumentException("CAN frame payload exceeds 8 bytes (not a raw frame): " + line);
    }

    int canId = (int) Long.parseLong(canIdHex, 16);
    byte[] data = hexToBytes(dataHex);

    return new ParsedFrame(canId, data, dataLength);
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

  private static final class ParsedFrame {
    private final int canId;
    private final byte[] data;
    private final int dataLength;

    private ParsedFrame(int canId, byte[] data, int dataLength) {
      this.canId = canId;
      this.data = data;
      this.dataLength = dataLength;
    }
  }
  private static final class RawFrame {
    private final int canId;
    private final boolean extended;
    private final String dataHex;

    private RawFrame(int canId, boolean extended, String dataHex) {
      this.canId = canId;
      this.extended = extended;
      this.dataHex = dataHex;
    }
  }

}