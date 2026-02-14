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

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonMergeAggregationStrategy implements AggregationStrategy {

  private final boolean prefixByTopic;

  public JsonMergeAggregationStrategy(boolean prefixByTopic) {
    this.prefixByTopic = prefixByTopic;
  }

  @Override
  public String getName() {
    return prefixByTopic ? "JsonMergeTopicPrefixed" : "JsonMergeFlat";
  }

  @Override
  public Message aggregate(AggregationContext context, Message[] contributions) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("meta", buildMeta(context));

    Map<String, Object> merged = new LinkedHashMap<>();
    AggregatorInputConfigDTO[] inputConfigs = context.getInputConfigs();

    for (int index = 0; index < contributions.length; index++) {
      Message message = contributions[index];
      if (message == null) {
        continue;
      }

      Map<String, Object> decoded = context.getMessageCodec().tryDecodeToJsonObject(message);
      if (decoded == null) {
        continue;
      }

      if (prefixByTopic) {
        merged.put(inputConfigs[index].getTopicName(), decoded);
      } else {
        merged.putAll(decoded);
      }
    }

    root.put("payload", merged);

    return context.getMessageBuilder().buildJsonMessage(root);
  }

  private Map<String, Object> buildMeta(AggregationContext context) {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("aggregator", context.getAggregatorName());
    meta.put("windowStartMillis", context.getWindowStartMillis());
    meta.put("windowEndMillis", context.getWindowEndMillis());
    meta.put("closedByAllInputs", context.isClosedByAllInputs());
    return meta;
  }
}
