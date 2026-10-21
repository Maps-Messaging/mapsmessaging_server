package io.mapsmessaging.aggregator.aggregate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnvelopeAggregationStrategyBranchCoverageTest {

  @Test
  void nullContributionsAreSkippedAndNullPayloadIsRepresentedExplicitly() {
    Message emptyPayload = new MessageBuilder()
        .setOpaqueData(null)
        .build();

    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/ignored", "/empty"},
        new Message[]{null, emptyPayload});

    JsonObject root = JsonParser.parseString(
        new String(result.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();

    assertEquals(1, root.getAsJsonArray("envelopes").size());
    JsonObject envelope = root.getAsJsonArray("envelopes").get(0).getAsJsonObject();
    assertEquals("/empty", envelope.get("topic").getAsString());
    assertTrue(envelope.get("payloadBase64").isJsonNull());
  }

  @Test
  void equalCorrelationAndSharedTypedDataArePromotedToAggregateMessage() {
    Map<String, TypedData> common = Map.of(
        "bytes", new TypedData(new byte[]{1, 2}),
        "count", new TypedData(3));

    Message first = new MessageBuilder()
        .setOpaqueData("{}".getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json")
        .setCorrelationData(new byte[]{9, 8})
        .setDataMap(common)
        .build();
    Message second = new MessageBuilder()
        .setOpaqueData("{}".getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json")
        .setCorrelationData(new byte[]{9, 8})
        .setDataMap(Map.of(
            "bytes", new TypedData(new byte[]{1, 2}),
            "count", new TypedData(3),
            "extra", new TypedData("x")))
        .build();

    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/a", "/b"}, new Message[]{first, second});

    assertArrayEquals(new byte[]{9, 8}, result.getCorrelationData());
    assertEquals(2, result.getDataMap().size());
    assertTrue(result.getDataMap().containsKey("bytes"));
    assertTrue(result.getDataMap().containsKey("count"));
  }

  @Test
  void correlationOrDataMismatchRemovesAggregateCommonValues() {
    Message first = new MessageBuilder()
        .setOpaqueData(new byte[]{1})
        .setCorrelationData(new byte[]{1})
        .setDataMap(Map.of("v", new TypedData(1)))
        .build();
    Message second = new MessageBuilder()
        .setOpaqueData(new byte[]{2})
        .setCorrelationData(new byte[]{2})
        .setDataMap(Map.of("v", new TypedData(2)))
        .build();

    Message result = new EnvelopeAggregationStrategy().aggregate(
        new String[]{"/a", "/b"}, new Message[]{first, second});

    assertNull(result.getCorrelationData());
    assertTrue(result.getDataMap().isEmpty());
  }
}