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
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.api.transformers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.transformers.jsonmutate.JsonMutator;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMutateTransformationDTO;
import java.nio.charset.StandardCharsets;

public class JsonMutateTransformation implements InterServerTransformation {

  private final JsonMutator mutator;

  public JsonMutateTransformation(JsonMutator mutator) {
    this.mutator = mutator;
  }

  @Override
  public ParsedMessage transform(String source, ParsedMessage message) {
    if (mutator == null) {
      return message;
    }

    try {
      byte[] data = message.getMessage().getOpaqueData();
      JsonElement element = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
      if (!element.isJsonObject()) {
        return message;
      }

      JsonObject mutated = mutator.apply(element.getAsJsonObject());

      MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
      messageBuilder.setOpaqueData(mutated.toString().getBytes(StandardCharsets.UTF_8));
      message.setMessage(messageBuilder.build());
      return message;
    } catch (Exception ignored) {
      return message;
    }
  }

  @Override
  public InterServerTransformation build(ConfigurationProperties properties) {
    return null;
  }

  public static JsonMutateTransformation fromDto(JsonMutateTransformationDTO dto) {
    if (dto == null || dto.getOperations() == null || dto.getOperations().isEmpty()) {
      return new JsonMutateTransformation(null);
    }
    return new JsonMutateTransformation(new JsonMutator(dto.getOperations()));
  }

  @Override
  public String getName() {
    return "json-mutate";
  }

  @Override
  public String getDescription() {
    return "Applies set/remove/rename JSON mutations using dot paths";
  }
}
