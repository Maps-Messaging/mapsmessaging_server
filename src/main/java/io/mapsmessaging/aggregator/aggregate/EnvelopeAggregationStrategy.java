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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnvelopeAggregationStrategy implements AggregationStrategy {

  private final Gson gson = new Gson();

  @Override
  public String getName() {
    return "Envelope";
  }

  @Override
  public Message aggregate(String[] topics, Message[] contributions) {
    Map<String, Object> root = new LinkedHashMap<>();
    Map<String, Object> inputs = new LinkedHashMap<>();
    for (int index = 0; index < contributions.length; index++) {
      Message message = contributions[index];
      if (message == null) {
        continue;
      }
      inputs.put(topics[index], buildEnvelopeEntry(message));
    }
    root.put("inputs", inputs);
    String json = gson.toJson(root);

    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setContentType("application/json");
    messageBuilder.setOpaqueData(json.getBytes());
    return messageBuilder.build();
  }

  private Map<String, Object> buildEnvelopeEntry(Message message) {
    Map<String, Object> entry = new LinkedHashMap<>();
    if(message.getContentType() != null && message.getContentType().equals("application/json")) {
      Type type = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> map = gson.fromJson(new String(message.getOpaqueData()), type);
      entry.put("payload", map);
    } else {
      entry.put("payloadBase64", Base64.getEncoder().encodeToString(message.getOpaqueData()));
    }
    return entry;
  }
}
