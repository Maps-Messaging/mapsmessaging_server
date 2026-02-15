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

package io.mapsmessaging.dto.rest.config.transformer.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.GeoHashResolverTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonQueryTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToValueTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToXmlTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.XmlToJsonTransformationDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TransformationConfigDtoTypeAdapterFactory implements TypeAdapterFactory {

  private static final String DISCRIMINATOR_FIELD = "type";

  private final Map<String, Class<? extends TransformationConfigDTO>> discriminatorMap;

  public TransformationConfigDtoTypeAdapterFactory() {
    Map<String, Class<? extends TransformationConfigDTO>> mapping = new HashMap<>();
    mapping.put(TransformationType.JSON_TO_XML.getWireName(), JsonToXmlTransformationDTO.class);
    mapping.put(TransformationType.XML_TO_JSON.getWireName(), XmlToJsonTransformationDTO.class);
    mapping.put(TransformationType.JSON_TO_VALUE.getWireName(), JsonToValueTransformationDTO.class);
    mapping.put(TransformationType.JSON_QUERY.getWireName(), JsonQueryTransformationDTO.class);
    mapping.put(TransformationType.GEOHASH.getWireName(), GeoHashResolverTransformationDTO.class);
    this.discriminatorMap = Map.copyOf(mapping);
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    Class<? super T> rawType = typeToken.getRawType();

    // Critical: only handle the base type, never subclasses.
    if (rawType != TransformationConfigDTO.class) {
      return null;
    }

    TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
    TypeAdapter<TransformationConfigDTO> baseDelegate = gson.getDelegateAdapter(this, TypeToken.get(TransformationConfigDTO.class));

    return new TypeAdapter<>() {

      @Override
      public void write(JsonWriter out, T value) throws IOException {
        // For base type, delegate. Subtypes should serialize normally via Gson.
        baseDelegate.write(out, (TransformationConfigDTO) value);
      }

      @Override
      public T read(JsonReader in) throws IOException {
        JsonElement jsonElement = jsonElementAdapter.read(in);
        if (!jsonElement.isJsonObject()) {
          throw new JsonParseException("TransformationConfigDTO must be a JSON object");
        }

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement discriminatorValue = jsonObject.get(DISCRIMINATOR_FIELD);
        if (discriminatorValue == null || !discriminatorValue.isJsonPrimitive()) {
          throw new JsonParseException("Missing required discriminator field '" + DISCRIMINATOR_FIELD + "'");
        }

        String typeTokenValue = discriminatorValue.getAsString();
        if (typeTokenValue == null) {
          throw new JsonParseException("Discriminator field '" + DISCRIMINATOR_FIELD + "' is null");
        }

        String normalized = typeTokenValue.trim().toLowerCase();

        // Canonicalize: ensure enum field parsing sees the lower-case token.
        jsonObject.addProperty(DISCRIMINATOR_FIELD, normalized);

        Class<? extends TransformationConfigDTO> targetClass = discriminatorMap.get(normalized);
        if (targetClass == null) {
          throw new JsonParseException("Unknown transformation type: '" + typeTokenValue + "'");
        }

        TypeAdapter<? extends TransformationConfigDTO> delegate =
            gson.getDelegateAdapter(TransformationConfigDtoTypeAdapterFactory.this, TypeToken.get(targetClass));

        @SuppressWarnings("unchecked")
        T cast = (T) delegate.fromJsonTree(jsonObject);
        return cast;
      }
    };
  }
}
