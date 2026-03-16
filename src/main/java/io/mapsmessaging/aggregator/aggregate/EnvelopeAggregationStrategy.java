/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License for the License at:
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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseMode;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EnvelopeAggregationStrategy implements AggregationStrategy {

  private static final String JSON_CONTENT_TYPE = "application/json";

  private final Gson gson = new Gson();

  @Override
  public String getName() {
    return "Envelope";
  }

  @Override
  public Message aggregate(String[] topics, Message[] contributions) {
    Map<String, Object> root = new LinkedHashMap<>();
    List<Map<String, Object>> envelopes = new ArrayList<>();
    for (int index = 0; index < contributions.length; index++) {
      Message message = contributions[index];
      if (message == null) {
        continue;
      }
      Map<String, Object> topicEntry = buildEnvelopeEntry(message);
      topicEntry.put("topic", topics[index]);
      envelopes.add(topicEntry);
    }
    root.put("envelopes", envelopes);
    String json = gson.toJson(root);

    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setContentType(JSON_CONTENT_TYPE);
    messageBuilder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));

    byte[] commonCorrelationData = computeCommonCorrelationData(contributions);
    if (commonCorrelationData != null) {
      messageBuilder.setCorrelationData(commonCorrelationData);
    }

    Map<String, TypedData> commonDataMap = computeCommonDataMap(contributions);
    if (commonDataMap != null && !commonDataMap.isEmpty()) {
      messageBuilder.setDataMap(commonDataMap);
    }

    return messageBuilder.build();
  }

  private Map<String, Object> buildEnvelopeEntry(Message message) {
    Map<String, Object> entry = new LinkedHashMap<>();

    byte[] opaqueData = message.getOpaqueData();
    if (opaqueData == null) {
      entry.put("payloadBase64", null);
      return entry;
    }

    String contentType = message.getContentType();
    if (contentType != null && contentType.equals(JSON_CONTENT_TYPE)) {
      Type type = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> map = gson.fromJson(new String(opaqueData, StandardCharsets.UTF_8), type);
      entry.put("payload", map);
    } else {
      if(message.getSchemaId() != null){
        try {
          MessageFormatter messageFormatter =  SchemaManager.getInstance().getMessageFormatter(message.getSchemaId());
          JsonObject json = messageFormatter.parseToJson(message.getOpaqueData(), ParseMode.STRICT);
          Type type = new TypeToken<Map<String, Object>>() {}.getType();
          Map<String, Object> map = gson.fromJson(json, type);
          entry.put("payload", map);
        } catch (IOException e) {
          entry.put("payloadBase64", Base64.getEncoder().encodeToString(opaqueData));
          // log it
        }
      }
      else {
        entry.put("payloadBase64", Base64.getEncoder().encodeToString(opaqueData));
      }
    }
    return entry;
  }

  private static byte[] computeCommonCorrelationData(Message[] contributions) {
    byte[] candidate = null;

    for (Message message : contributions) {
      if (message == null) {
        continue;
      }

      byte[] correlationData = message.getCorrelationData();
      if (correlationData == null) {
        return null;
      }

      if (candidate == null) {
        candidate = correlationData;
        continue;
      }

      if (!Arrays.equals(candidate, correlationData)) {
        return null;
      }
    }

    return candidate;
  }

  private static Map<String, TypedData> computeCommonDataMap(Message[] contributions) {
    Message firstMessage = firstNonNull(contributions);
    if (firstMessage == null) {
      return null;
    }

    Map<String, TypedData> firstDataMap = firstMessage.getDataMap();
    if (firstDataMap == null || firstDataMap.isEmpty()) {
      return null;
    }

    Map<String, TypedData> intersection = new LinkedHashMap<>();

    for (Map.Entry<String, TypedData> entry : firstDataMap.entrySet()) {
      String key = entry.getKey();
      TypedData candidate = entry.getValue();
      if (candidate == null) {
        continue;
      }

      boolean matchesAll = true;

      for (Message message : contributions) {
        if (message == null) {
          continue;
        }

        Map<String, TypedData> dataMap = message.getDataMap();
        if (dataMap == null) {
          matchesAll = false;
          break;
        }

        TypedData other = dataMap.get(key);
        if (!typedDataEquals(candidate, other)) {
          matchesAll = false;
          break;
        }
      }

      if (matchesAll) {
        intersection.put(key, candidate);
      }
    }

    return intersection;
  }

  private static Message firstNonNull(Message[] contributions) {
    for (Message message : contributions) {
      if (message != null) {
        return message;
      }
    }
    return null;
  }

  private static boolean typedDataEquals(TypedData left, TypedData right) {
    if (left == right) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    if (left.getType() != right.getType()) {
      return false;
    }

    Object leftData = left.getData();
    Object rightData = right.getData();

    if (leftData == rightData) {
      return true;
    }
    if (leftData == null || rightData == null) {
      return false;
    }

    return switch (left.getType()) {
      case BYTE_ARRAY -> Arrays.equals((byte[]) leftData, (byte[]) rightData);
      case SHORT_ARRAY -> Arrays.equals((short[]) leftData, (short[]) rightData);
      case INT_ARRAY -> Arrays.equals((int[]) leftData, (int[]) rightData);
      case LONG_ARRAY -> Arrays.equals((long[]) leftData, (long[]) rightData);
      case FLOAT_ARRAY -> Arrays.equals((float[]) leftData, (float[]) rightData);
      case DOUBLE_ARRAY -> Arrays.equals((double[]) leftData, (double[]) rightData);
      case STRING_ARRAY -> Arrays.equals((String[]) leftData, (String[]) rightData);
      case CHAR_ARRAY -> Arrays.equals((char[]) leftData, (char[]) rightData);
      case TYPED_MAP -> Objects.equals(leftData, rightData);
      default -> Objects.equals(leftData, rightData);
    };
  }
}
