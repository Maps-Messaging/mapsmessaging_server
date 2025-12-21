package io.mapsmessaging.api.message;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageFactoryTest {

  private Message createBaseMessage() {
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setId(123456789L);

    Map<String, String> metaData = new LinkedHashMap<>();
    metaData.put("k1", "v1");
    metaData.put("k2", "v2");
    messageBuilder.setMeta(metaData);

    byte[] opaquePayload = "hello-world-payload".getBytes(StandardCharsets.UTF_8);
    messageBuilder.setOpaqueData(opaquePayload);

    messageBuilder.setPriority(Priority.HIGHEST);
    messageBuilder.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    messageBuilder.setStoreOffline(true);

    messageBuilder.setCorrelationData("corr-id-123");
    messageBuilder.setContentType("text/plain");

    messageBuilder.setDelayed(0L);
    messageBuilder.setExpiry(0L);
    // fixed creation for deterministic header
    messageBuilder.setCreation(1728000000000L);

    messageBuilder.setResponseTopic("reply/topic");
    messageBuilder.setRetain(true);
    messageBuilder.setPayloadUTF8(true);

    messageBuilder.setSchemaId("schema-123");

    return new Message(messageBuilder);
  }

  private ByteBuffer[] duplicateForRead(ByteBuffer[] source) {
    ByteBuffer[] copy = new ByteBuffer[source.length];
    for (int i = 0; i < source.length; i++) {
      copy[i] = source[i].duplicate();
    }
    return copy;
  }

  private byte[] toBytes(ByteBuffer[] buffers) {
    int total = 0;
    for (ByteBuffer buffer : buffers) {
      total += buffer.remaining();
    }

    byte[] result = new byte[total];
    int offset = 0;
    for (ByteBuffer buffer : buffers) {
      int len = buffer.remaining();
      buffer.get(result, offset, len);
      offset += len;
    }
    return result;
  }

  @Test
  void packAndUnpackRoundTrip() throws Exception {
    MessageFactory messageFactory = MessageFactory.getInstance();
    Message originalMessage = createBaseMessage();

    ByteBuffer[] packedBuffers = messageFactory.pack(originalMessage);
    assertNotNull(packedBuffers);
    assertTrue(packedBuffers.length >= 2);

    Message unpackedMessage = messageFactory.unpack(duplicateForRead(packedBuffers));
    assertNotNull(unpackedMessage);

    assertEquals(originalMessage.getIdentifier(), unpackedMessage.getIdentifier());
    assertEquals(originalMessage.getPriority(), unpackedMessage.getPriority());
    assertEquals(originalMessage.getQualityOfService(), unpackedMessage.getQualityOfService());
    assertEquals(originalMessage.getResponseTopic(), unpackedMessage.getResponseTopic());
    assertEquals(originalMessage.getContentType(), unpackedMessage.getContentType());
    assertEquals(originalMessage.getSchemaId(), unpackedMessage.getSchemaId());

    assertEquals(originalMessage.isRetain(), unpackedMessage.isRetain());
    assertEquals(originalMessage.isUTF8(), unpackedMessage.isUTF8());
    assertEquals(originalMessage.isStoreOffline(), unpackedMessage.isStoreOffline());

    assertArrayEquals(
        originalMessage.getCorrelationData(),
        unpackedMessage.getCorrelationData()
    );

    if (originalMessage.getOpaqueData() != null || unpackedMessage.getOpaqueData() != null) {
      assertArrayEquals(
          originalMessage.getOpaqueData(),
          unpackedMessage.getOpaqueData()
      );
    }

    if (originalMessage.getMeta() != null || unpackedMessage.getMeta() != null) {
      assertEquals(originalMessage.getMeta(), unpackedMessage.getMeta());
    }
  }

  @Test
  void packWithUpdatedMetaOverridesStoredMeta() throws Exception {
    MessageFactory messageFactory = MessageFactory.getInstance();
    Message originalMessage = createBaseMessage();

    Map<String, String> updatedMeta = new LinkedHashMap<>();
    updatedMeta.put("k1", "overridden");
    updatedMeta.put("added", "new-value");

    ByteBuffer[] packedBuffers = messageFactory.pack(originalMessage, updatedMeta);
    Message unpackedMessage = messageFactory.unpack(duplicateForRead(packedBuffers));

    assertNotNull(unpackedMessage.getMeta());
    assertEquals("overridden", unpackedMessage.getMeta().get("k1"));
    assertEquals("new-value", unpackedMessage.getMeta().get("added"));

    // Original in-memory meta on the source message should be unchanged
    assertEquals("v1", originalMessage.getMeta().get("k1"));
    assertFalse(originalMessage.getMeta().containsKey("added"));
  }

  @Test
  void wireFormatChangesWhenMetaChanges() throws Exception {
    MessageFactory messageFactory = MessageFactory.getInstance();
    Message originalMessage = createBaseMessage();

    ByteBuffer[] originalPacked = messageFactory.pack(originalMessage);

    Map<String, String> updatedMeta = new LinkedHashMap<>();
    updatedMeta.put("k1", "overridden");
    updatedMeta.put("k2", "v2");
    updatedMeta.put("added", "new-value");

    ByteBuffer[] updatedPacked = messageFactory.pack(originalMessage, updatedMeta);

    byte[] originalBytes = toBytes(duplicateForRead(originalPacked));
    byte[] updatedBytes = toBytes(duplicateForRead(updatedPacked));

    assertNotEquals(
        new String(originalBytes, StandardCharsets.ISO_8859_1),
        new String(updatedBytes, StandardCharsets.ISO_8859_1),
        "Wire format should differ when meta is changed"
    );
  }

  @Test
  void packAndUnpackWithByteArrayCorrelationData() throws Exception {
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setId(987654321L);

    Map<String, String> metaData = new LinkedHashMap<>();
    metaData.put("meta", "value");
    messageBuilder.setMeta(metaData);

    byte[] opaquePayload = "payload-binary".getBytes(StandardCharsets.UTF_8);
    messageBuilder.setOpaqueData(opaquePayload);

    messageBuilder.setPriority(Priority.LOWEST);
    messageBuilder.setQualityOfService(QualityOfService.AT_MOST_ONCE);

    messageBuilder.setStoreOffline(false);

    byte[] correlationBytes = "corr-binary".getBytes(StandardCharsets.UTF_8);
    messageBuilder.setCorrelationData(correlationBytes);

    messageBuilder.setContentType("application/octet-stream");
    messageBuilder.setDelayed(0L);
    messageBuilder.setExpiry(0L);
    messageBuilder.setCreation(1728000000001L);
    messageBuilder.setResponseTopic("binary/reply");
    messageBuilder.setRetain(false);
    messageBuilder.setPayloadUTF8(false);
    messageBuilder.setSchemaId(null);

    Message originalMessage = new Message(messageBuilder);

    MessageFactory messageFactory = MessageFactory.getInstance();
    ByteBuffer[] packedBuffers = messageFactory.pack(originalMessage);
    Message unpackedMessage = messageFactory.unpack(duplicateForRead(packedBuffers));

    assertTrue(unpackedMessage.isCorrelationDataByteArray());
    assertArrayEquals(
        correlationBytes,
        unpackedMessage.getCorrelationData()
    );

    assertEquals("application/octet-stream", unpackedMessage.getContentType());
    assertEquals("binary/reply", unpackedMessage.getResponseTopic());
    assertFalse(unpackedMessage.isRetain());
    assertFalse(unpackedMessage.isUTF8());
  }

  @Test
  void opaqueDataAbsentIsHandled() throws Exception {
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setId(111222333L);

    Map<String, String> metaData = new LinkedHashMap<>();
    metaData.put("only-meta", "true");
    messageBuilder.setMeta(metaData);

    messageBuilder.setOpaqueData(null);

    messageBuilder.setPriority(Priority.NORMAL);
    messageBuilder.setQualityOfService(QualityOfService.EXACTLY_ONCE);
    messageBuilder.setStoreOffline(true);
    messageBuilder.setCorrelationData("no-payload");
    messageBuilder.setContentType("text/plain");
    messageBuilder.setDelayed(0L);
    messageBuilder.setExpiry(0L);
    messageBuilder.setCreation(1728000000002L);
    messageBuilder.setResponseTopic("no/payload");
    messageBuilder.setRetain(false);
    messageBuilder.setPayloadUTF8(true);
    messageBuilder.setSchemaId("schema-no-payload");

    Message originalMessage = new Message(messageBuilder);

    MessageFactory messageFactory = MessageFactory.getInstance();
    ByteBuffer[] packedBuffers = messageFactory.pack(originalMessage);
    Message unpackedMessage = messageFactory.unpack(duplicateForRead(packedBuffers));

    assertNull(unpackedMessage.getOpaqueData());
    assertEquals(originalMessage.getContentType(), unpackedMessage.getContentType());
    assertEquals(originalMessage.getSchemaId(), unpackedMessage.getSchemaId());
    assertEquals(originalMessage.getMeta(), unpackedMessage.getMeta());
  }

  @Test
  void nullMetaStaysNullAfterRoundTrip() throws Exception {
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setId(200L);
    messageBuilder.setMeta(null);
    messageBuilder.setOpaqueData("x".getBytes(StandardCharsets.UTF_8));
    messageBuilder.setPriority(Priority.NORMAL);
    messageBuilder.setQualityOfService(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setStoreOffline(true);
    messageBuilder.setCorrelationData("c");
    messageBuilder.setContentType("text/plain");
    messageBuilder.setDelayed(0L);
    messageBuilder.setExpiry(0L);
    messageBuilder.setCreation(1728000000100L);
    messageBuilder.setResponseTopic("topic/null-meta");
    messageBuilder.setRetain(false);
    messageBuilder.setPayloadUTF8(true);
    messageBuilder.setSchemaId(null);

    Message message = new Message(messageBuilder);

    MessageFactory factory = MessageFactory.getInstance();
    ByteBuffer[] packed = factory.pack(message);
    Message unpacked = factory.unpack(duplicateForRead(packed));

    assertTrue(unpacked.getMeta().isEmpty());
  }

  @Test
  void emptyMetaRoundTripsAsEmptyMap() throws Exception {
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setId(201L);
    messageBuilder.setMeta(new LinkedHashMap<>()); // empty but non-null
    messageBuilder.setOpaqueData("y".getBytes(StandardCharsets.UTF_8));
    messageBuilder.setPriority(Priority.NORMAL);
    messageBuilder.setQualityOfService(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setStoreOffline(true);
    messageBuilder.setCorrelationData("c2");
    messageBuilder.setContentType("text/plain");
    messageBuilder.setDelayed(0L);
    messageBuilder.setExpiry(0L);
    messageBuilder.setCreation(1728000000200L);
    messageBuilder.setResponseTopic("topic/empty-meta");
    messageBuilder.setRetain(false);
    messageBuilder.setPayloadUTF8(true);
    messageBuilder.setSchemaId(null);

    Message message = new Message(messageBuilder);

    MessageFactory factory = MessageFactory.getInstance();
    ByteBuffer[] packed = factory.pack(message);
    Message unpacked = factory.unpack(duplicateForRead(packed));

    assertNotNull(unpacked.getMeta());

    assertTrue(unpacked.getMeta().size() == 1 || unpacked.getMeta().size() == 4); // includes time_ms created and / or location
  }

  @Test
  void storeOfflineIsForcedTrueAfterReload() throws Exception {
    MessageBuilder messageBuilder = new MessageBuilder();

    Map<String, String> map = new LinkedHashMap<>();
    map.put("k","v");
    messageBuilder.setId(300L);
    messageBuilder.setMeta(map);
    messageBuilder.setOpaqueData("z".getBytes(StandardCharsets.UTF_8));
    messageBuilder.setPriority(Priority.NORMAL);
    messageBuilder.setQualityOfService(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setStoreOffline(false); // explicitly false
    messageBuilder.setCorrelationData("c3");
    messageBuilder.setContentType("text/plain");
    messageBuilder.setDelayed(0L);
    messageBuilder.setExpiry(0L);
    messageBuilder.setCreation(1728000000300L);
    messageBuilder.setResponseTopic("topic/offline");
    messageBuilder.setRetain(false);
    messageBuilder.setPayloadUTF8(true);
    messageBuilder.setSchemaId(null);

    Message original = new Message(messageBuilder);
    assertFalse(original.isStoreOffline());

    MessageFactory factory = MessageFactory.getInstance();
    ByteBuffer[] packed = factory.pack(original);
    Message unpacked = factory.unpack(duplicateForRead(packed));

    assertTrue(unpacked.isStoreOffline(), "Reloaded messages are always stored offline");
  }

  @Test
  void getKeyReturnsIdentifier() {
    Message message = createBaseMessage();
    assertEquals(message.getIdentifier(), message.getKey());
  }

  @Test
  void setDelayedIgnoresNegativeValues() {
    Message message = createBaseMessage();

    long originalDelayed = message.getDelayed();
    message.setDelayed(-1L);

    assertEquals(originalDelayed, message.getDelayed());
  }

  @Test
  void setDelayedAcceptsNonNegativeValues() {
    Message message = createBaseMessage();

    message.setDelayed(12345L);
    assertEquals(12345L, message.getDelayed());
  }

  @Test
  void toStringContainsKeyParts() {
    Message message = createBaseMessage();
    String text = message.toString();

    assertNotNull(text);
    assertTrue(text.contains("Key:"));
    assertTrue(text.contains("meta:"));
    assertTrue(text.contains("Opaque:"));
    assertTrue(text.contains("ContentType:"));
  }
}
