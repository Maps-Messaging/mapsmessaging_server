package io.mapsmessaging.aggregator.aggregate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnvelopeAggregationStrategyFinalCoverageTest {

  @Test
  void jsonContentTypeEmbedsPayloadAsJsonObject() {
    Message message = new MessageBuilder()
        .setOpaqueData("{\"value\":7}".getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json")
        .build();

    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/json"}, new Message[]{message});

    JsonObject envelope = JsonParser.parseString(
        new String(result.getOpaqueData(), StandardCharsets.UTF_8))
        .getAsJsonObject()
        .getAsJsonArray("envelopes")
        .get(0)
        .getAsJsonObject();

    assertEquals(7, envelope.getAsJsonObject("payload").get("value").getAsInt());
    assertFalse(envelope.has("payloadBase64"));
  }

  @Test
  void binaryPayloadWithoutSchemaIsEncodedAsBase64() {
    Message message = new MessageBuilder()
        .setOpaqueData(new byte[]{1, 2, 3})
        .setContentType("application/octet-stream")
        .build();

    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/binary"}, new Message[]{message});

    JsonObject envelope = JsonParser.parseString(
        new String(result.getOpaqueData(), StandardCharsets.UTF_8))
        .getAsJsonObject()
        .getAsJsonArray("envelopes")
        .get(0)
        .getAsJsonObject();

    assertEquals("AQID", envelope.get("payloadBase64").getAsString());
  }

  @Test
  void allNullContributionsProduceEmptyEnvelopeWithoutCommonMetadata() {
    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/a", "/b"}, new Message[]{null, null});

    JsonObject root = JsonParser.parseString(
        new String(result.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();

    assertTrue(root.getAsJsonArray("envelopes").isEmpty());
    assertNull(result.getCorrelationData());
    assertTrue(result.getDataMap().isEmpty());
  }

  @Test
  void commonTypedArrayValuesSurviveIntersectionAcrossMessages() {
    Map<String, TypedData> firstMap = typedArrays();
    Map<String, TypedData> secondMap = typedArrays();

    Message first = new MessageBuilder().setOpaqueData(new byte[]{1}).setDataMap(firstMap).build();
    Message second = new MessageBuilder().setOpaqueData(new byte[]{2}).setDataMap(secondMap).build();

    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/a", "/b"}, new Message[]{first, second});

    assertEquals(firstMap.keySet(), result.getDataMap().keySet());
  }

  private static Map<String, TypedData> typedArrays() {
    Map<String, TypedData> values = new LinkedHashMap<>();
    values.put("shorts", new TypedData(new short[]{1, 2}));
    values.put("ints", new TypedData(new int[]{1, 2}));
    values.put("longs", new TypedData(new long[]{1, 2}));
    values.put("floats", new TypedData(new float[]{1, 2}));
    values.put("doubles", new TypedData(new double[]{1, 2}));
    values.put("strings", new TypedData(new String[]{"a", "b"}));
    values.put("chars", new TypedData(new char[]{'a', 'b'}));
    return values;
  }
}