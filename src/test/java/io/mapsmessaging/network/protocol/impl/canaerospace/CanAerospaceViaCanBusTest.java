package io.mapsmessaging.network.protocol.impl.canaerospace;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
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

class CanAerospaceViaCanBusTest extends BaseTestConfig {

  @Test
  void testValidCanaerospaceFramesAreDecodedAndPublished() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch messageLatch = new CountDownLatch(2);
    List<ReceivedMessage> receivedMessages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      String topicName = messageEvent.getDestinationName();
      receivedMessages.add(new ReceivedMessage(topicName, messageEvent.getMessage()));
      messageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("canaerospaceValidFramesTest" + System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContext subscriptionContext = new SubscriptionContextBuilder("/vcan1/#", ClientAcknowledgement.AUTO)
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(subscriptionContext);

      delay(100);

      injectRawCanFramesIntoVcan("vcan1", new RawFrame[]{
          new RawFrame(0x137, false, "0106000000000064"),
          new RawFrame(0x138, false, "01060000000000C8")
      });

      boolean received = messageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "Expected decoded CANAerospace messages to be published");
      Assertions.assertEquals(2, receivedCount.get(), "Expected one published message per injected valid CANAerospace frame");

      session.removeSubscription(subscriptionContext.getKey());

      int attempts = 0;
      do {
        delay(100);
        attempts++;
      }
      while (receivedMessages.size() < 2 && attempts < 20);

      Assertions.assertEquals(2, receivedMessages.size(), "Expected exactly two published messages");

      for (ReceivedMessage receivedMessage : receivedMessages) {
        Assertions.assertFalse(
            receivedMessage.topicName.endsWith("/unknown"),
            "Valid CANAerospace frame should not be routed to unknown topic: " + receivedMessage.topicName
        );
        validateDecodedCanaerospacePayload(receivedMessage.message);
      }
    }
    finally {
      close(session);
    }
  }

  @Test
  void testUnknownEightByteFrameRoutesToUnknownTopic() throws LoginException, IOException, InterruptedException {
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch messageLatch = new CountDownLatch(1);
    List<ReceivedMessage> receivedMessages = new CopyOnWriteArrayList<>();

    MessageListener listener = messageEvent -> {
      receivedCount.incrementAndGet();
      String topicName = messageEvent.getDestinationName();
      receivedMessages.add(new ReceivedMessage(topicName, messageEvent.getMessage()));
      messageLatch.countDown();
      messageEvent.getCompletionTask().run();
    };

    Session session = createSession("canaerospaceUnknownEightByteTest" + System.nanoTime(), 60, 60, false, listener);
    Assertions.assertNotNull(session);

    try {
      SubscriptionContext subscriptionContext = new SubscriptionContextBuilder("/vcan1/#", ClientAcknowledgement.AUTO)
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(subscriptionContext);

      delay(100);

      RawFrame unknownFrame = new RawFrame(0x18FF9999, true, "0102030405060708");
      injectRawCanFramesIntoVcan("vcan1", new RawFrame[]{unknownFrame});

      boolean received = messageLatch.await(5, TimeUnit.SECONDS);
      Assertions.assertTrue(received, "Expected unknown 8-byte frame to be published");
      Assertions.assertEquals(1, receivedCount.get(), "Expected one published message");

      session.removeSubscription(subscriptionContext.getKey());

      int attempts = 0;
      do {
        delay(100);
        attempts++;
      }
      while (receivedMessages.size() < 1 && attempts < 20);

      Assertions.assertEquals(1, receivedMessages.size(), "Expected exactly one published message");

      ReceivedMessage receivedMessage = receivedMessages.get(0);
      Assertions.assertTrue(
          receivedMessage.topicName.endsWith("/unknown"),
          "Unknown CANAerospace frame should route to unknown topic: " + receivedMessage.topicName
      );

      validateUnknownPayload(receivedMessage.message, hexToBytes(unknownFrame.dataHex));
    }
    finally {
      close(session);
    }
  }

  private void validateDecodedCanaerospacePayload(Message message) {
    Assertions.assertNotNull(message, "Message must not be null");

    byte[] payloadBytes = message.getOpaqueData();
    Assertions.assertNotNull(payloadBytes, "Message payload must not be null");
    Assertions.assertTrue(payloadBytes.length > 0, "Message payload must not be empty");

    String payload = new String(payloadBytes, StandardCharsets.UTF_8).trim();
    Assertions.assertFalse(payload.isEmpty(), "Message payload must not be blank");

    JsonElement root;
    try {
      root = JsonParser.parseString(payload);
    }
    catch (Exception exception) {
      Assertions.fail("Payload is not valid JSON. Payload: " + payload, exception);
      return;
    }

    Assertions.assertTrue(root.isJsonObject(), "Payload must be a JSON object. Payload: " + payload);
    JsonObject object = root.getAsJsonObject();

    assertFieldPresent(object, "canId");
    assertFieldPresent(object, "dlc");
    assertFieldPresent(object, "extended");
    assertFieldPresent(object, "data");
    assertFieldPresent(object, "canaerospace");

    int dlc = object.get("dlc").getAsInt();
    Assertions.assertEquals(8, dlc, "Decoded CANAerospace payload must have dlc=8. Payload: " + payload);

    String data = object.get("data").getAsString();
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(data);
    }
    catch (IllegalArgumentException exception) {
      Assertions.fail("data is not valid Base64. Payload: " + payload, exception);
      return;
    }

    Assertions.assertEquals(8, decoded.length, "Decoded payload length must match dlc. Payload: " + payload);

    JsonObject canaerospace = object.getAsJsonObject("canaerospace");
    assertFieldPresent(canaerospace, "nodeId");
    assertFieldPresent(canaerospace, "payloadDataTypeNumber");
    assertFieldPresent(canaerospace, "serviceCode");
    assertFieldPresent(canaerospace, "messageCode");

    int nodeId = canaerospace.get("nodeId").getAsInt();
    int payloadDataTypeNumber = canaerospace.get("payloadDataTypeNumber").getAsInt();
    int serviceCode = canaerospace.get("serviceCode").getAsInt();
    int messageCode = canaerospace.get("messageCode").getAsInt();

    Assertions.assertTrue(nodeId >= 0 && nodeId <= 255, "nodeId must be in [0..255]. Payload: " + payload);
    Assertions.assertTrue(payloadDataTypeNumber >= 0 && payloadDataTypeNumber <= 255, "payloadDataTypeNumber must be in [0..255]. Payload: " + payload);
    Assertions.assertTrue(serviceCode >= 0 && serviceCode <= 255, "serviceCode must be in [0..255]. Payload: " + payload);
    Assertions.assertTrue(messageCode >= 0 && messageCode <= 255, "messageCode must be in [0..255]. Payload: " + payload);
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
    String data = object.get("data").getAsString();

    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(data);
    }
    catch (IllegalArgumentException exception) {
      Assertions.fail("data is not valid Base64. Payload: " + payload, exception);
      return;
    }

    Assertions.assertEquals(dlc, decoded.length, "Decoded data length must match dlc. Payload: " + payload);
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
  }

  private byte[] hexToBytes(String hex) {
    int length = hex.length();
    byte[] bytes = new byte[length / 2];

    for (int index = 0; index < length; index += 2) {
      int value = Integer.parseInt(hex.substring(index, index + 2), 16);
      bytes[index / 2] = (byte) value;
    }

    return bytes;
  }

  private static final class ReceivedMessage {
    private final String topicName;
    private final Message message;

    private ReceivedMessage(String topicName, Message message) {
      this.topicName = topicName;
      this.message = message;
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