/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.aggregator.aggregate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnvelopeAggregationStrategyTest {

  private static final String JSON_CONTENT_TYPE = "application/json";

  private final EnvelopeAggregationStrategy strategy = new EnvelopeAggregationStrategy();
  private final Gson gson = new Gson();
  private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

  @Test
  void aggregate_jsonAndBinaryContributions_preservesTopicsAndPayloadForms() {
    Message jsonMessage = new MessageBuilder()
        .setContentType(JSON_CONTENT_TYPE)
        .setOpaqueData("{\"temperature\":21}".getBytes(StandardCharsets.UTF_8))
        .build();
    Message binaryMessage = new MessageBuilder()
        .setOpaqueData(new byte[]{1, 2, 3})
        .build();

    Message result = strategy.aggregate(
        new String[]{"/sensor/json", "/sensor/binary", "/sensor/missing"},
        new Message[]{jsonMessage, binaryMessage, null}
    );

    assertEquals(JSON_CONTENT_TYPE, result.getContentType());
    List<Map<String, Object>> envelopes = envelopes(result);
    assertEquals(2, envelopes.size());
    assertEquals("/sensor/json", envelopes.get(0).get("topic"));
    assertEquals(21.0, payload(envelopes.get(0)).get("temperature"));
    assertEquals("/sensor/binary", envelopes.get(1).get("topic"));
    assertEquals("AQID", envelopes.get(1).get("payloadBase64"));
  }

  @Test
  void aggregate_matchingCorrelationAndDataMap_propagatesOnlyCommonValues() {
    Map<String, TypedData> firstData = new LinkedHashMap<>();
    firstData.put("commonText", new TypedData("shared"));
    firstData.put("commonBytes", new TypedData(new byte[]{4, 5}));
    firstData.put("different", new TypedData(1));

    Map<String, TypedData> secondData = new LinkedHashMap<>();
    secondData.put("commonText", new TypedData("shared"));
    secondData.put("commonBytes", new TypedData(new byte[]{4, 5}));
    secondData.put("different", new TypedData(2));

    Message first = messageWithCommonData(firstData, new byte[]{9, 8});
    Message second = messageWithCommonData(secondData, new byte[]{9, 8});

    Message result = strategy.aggregate(new String[]{"one", "two"}, new Message[]{first, second});

    assertArrayEquals(new byte[]{9, 8}, result.getCorrelationData());
    assertEquals("shared", result.getDataMap().get("commonText").getData());
    assertArrayEquals(new byte[]{4, 5}, (byte[]) result.getDataMap().get("commonBytes").getData());
    assertFalse(result.getDataMap().containsKey("different"));
  }

  @Test
  void aggregate_differentCorrelation_doesNotPropagateCorrelation() {
    Message first = messageWithCommonData(Map.of(), new byte[]{1});
    Message second = messageWithCommonData(Map.of(), new byte[]{2});

    Message result = strategy.aggregate(new String[]{"one", "two"}, new Message[]{first, second});

    assertNull(result.getCorrelationData());
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> envelopes(Message result) {
    String json = new String(result.getOpaqueData(), StandardCharsets.UTF_8);
    Map<String, Object> root = gson.fromJson(json, mapType);
    return (List<Map<String, Object>>) root.get("envelopes");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> payload(Map<String, Object> envelope) {
    return (Map<String, Object>) envelope.get("payload");
  }

  private Message messageWithCommonData(Map<String, TypedData> dataMap, byte[] correlationData) {
    return new MessageBuilder()
        .setOpaqueData(new byte[]{7})
        .setDataMap(dataMap)
        .setCorrelationData(correlationData)
        .build();
  }
}
