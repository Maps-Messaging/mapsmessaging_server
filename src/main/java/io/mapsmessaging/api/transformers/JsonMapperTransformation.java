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

package io.mapsmessaging.api.transformers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.transformers.jsonmapper.JsonMapper;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMapperTransformationDTO;


import java.nio.charset.StandardCharsets;

public class JsonMapperTransformation implements InterServerTransformation {

  private final JsonMapper mapper;

  public JsonMapperTransformation() {
    mapper = null;
  }

  public JsonMapperTransformation(JsonMapper mutator) {
    this.mapper = mutator;
  }

  @Override
  public ParsedMessage transform(String source, ParsedMessage message) {
    if (mapper == null) {
      return message;
    }

    try {
      byte[] data = message.getMessage().getOpaqueData();
      JsonElement element = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
      if (!element.isJsonObject()) {
        return message;
      }
      JsonObject base = element.getAsJsonObject();
      JsonObject payload  = base.getAsJsonArray("envelopes").get(0).getAsJsonObject().getAsJsonObject("payload");
      JsonObject mutated = mapper.apply(payload.getAsJsonObject());

      MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
      messageBuilder.setOpaqueData(mutated.toString().getBytes(StandardCharsets.UTF_8));
      message.setMessage(messageBuilder.build());
      return message;
    } catch (Exception ignored) {
      return message;
    }
  }

  @Override
  public InterServerTransformation build(TransformationConfigDTO dto) {
    if( !(dto instanceof JsonMapperTransformationDTO jsonMapperTransformationDTO)){
      return null;
    }
    if (jsonMapperTransformationDTO.getOperations() == null || jsonMapperTransformationDTO.getOperations().isEmpty()) {
      return new JsonMapperTransformation(null);
    }
    return new JsonMapperTransformation(new JsonMapper(jsonMapperTransformationDTO.getOperations()));
  }

  @Override
  public String getName() {
    return "jsonmapper";
  }

  @Override
  public String getDescription() {
    return "Copies from the source and creates a new destination message with the result of the JsonMapper";
  }
}
