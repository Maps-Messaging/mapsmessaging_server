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

package io.mapsmessaging.dto.rest.config.transformer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.dto.rest.config.transformer.gson.TransformationConfigDtoTypeAdapterFactory;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventEnvelopeTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventJsonTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventNativeTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.GeoHashResolverTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMapperTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonMutateTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonQueryTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToValueTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToXmlTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.SchemaToJsonTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.XmlToJsonTransformationDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationConfigDtoGsonPolymorphismTest {

  private static Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapterFactory(new TransformationConfigDtoTypeAdapterFactory())
        .create();
  }

  @ParameterizedTest
  @MethodSource("transformationConfigurations")
  void discriminatorDeserializesToExpectedConfigurationDto(
      String json,
      Class<? extends TransformationConfigDTO> expectedClass,
      TransformationType expectedType) {

    Gson gson = buildGson();

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    assertNotNull(dto);
    assertInstanceOf(expectedClass, dto);
    assertEquals(expectedType, dto.getType());
  }

  @ParameterizedTest
  @MethodSource("transformationConfigurations")
  void discriminatorIsCaseInsensitive(
      String json,
      Class<? extends TransformationConfigDTO> expectedClass,
      TransformationType expectedType) {

    Gson gson = buildGson();
    String mixedCaseJson = json.replace(
        expectedType.getWireName().toLowerCase(Locale.ROOT),
        toMixedCase(expectedType.getWireName()));

    TransformationConfigDTO dto = gson.fromJson(mixedCaseJson, TransformationConfigDTO.class);

    assertNotNull(dto);
    assertInstanceOf(expectedClass, dto);
    assertEquals(expectedType, dto.getType());
  }

  @Test
  void deserializeMixedListParsesAllConcreteTypes() {
    Gson gson = buildGson();
    Type listType = new TypeToken<List<TransformationConfigDTO>>() {}.getType();

    String json = """
        [
          {"type":"jsontoxml"},
          {"type":"xmltojson"},
          {"type":"jsontoschema","schemaName":"example"},
          {"type":"schematojson"},
          {"type":"jsontovalue","key":"data.temperature"},
          {"type":"jsonquery","query":"."},
          {
            "type":"geohash",
            "prefix":"maps/location",
            "latKey":"latitude",
            "lonKey":"longitude",
            "precision":7,
            "units":"deg",
            "layout":"raw",
            "onMissing":"skip",
            "latKeys":["lat","gps.lat"],
            "lonKeys":["lon","gps.lon"]
          },
          {"type":"jsonmutate","operations":[]},
          {"type":"jsonmapper","operations":[]},
          {"type":"cloudevent-envelope"},
          {"type":"cloudevent-json"},
          {"type":"cloudevent-native"}
        ]
        """;

    List<TransformationConfigDTO> list = gson.fromJson(json, listType);

    assertNotNull(list);
    assertEquals(12, list.size());

    assertInstanceOf(JsonToXmlTransformationDTO.class, list.get(0));
    assertInstanceOf(XmlToJsonTransformationDTO.class, list.get(1));
    assertInstanceOf(JsonToSchemaTransformationDTO.class, list.get(2));
    assertInstanceOf(SchemaToJsonTransformationDTO.class, list.get(3));
    assertInstanceOf(JsonToValueTransformationDTO.class, list.get(4));
    assertInstanceOf(JsonQueryTransformationDTO.class, list.get(5));
    assertInstanceOf(GeoHashResolverTransformationDTO.class, list.get(6));
    assertInstanceOf(JsonMutateTransformationDTO.class, list.get(7));
    assertInstanceOf(JsonMapperTransformationDTO.class, list.get(8));
    assertInstanceOf(CloudEventEnvelopeTransformationDTO.class, list.get(9));
    assertInstanceOf(CloudEventJsonTransformationDTO.class, list.get(10));
    assertInstanceOf(CloudEventNativeTransformationDTO.class, list.get(11));

    assertEquals(TransformationType.JSON_TO_XML, list.get(0).getType());
    assertEquals(TransformationType.XML_TO_JSON, list.get(1).getType());
    assertEquals(TransformationType.JSON_TO_SCHEMA, list.get(2).getType());
    assertEquals(TransformationType.SCHEMA_TO_JSON, list.get(3).getType());
    assertEquals(TransformationType.JSON_TO_VALUE, list.get(4).getType());
    assertEquals(TransformationType.JSON_QUERY, list.get(5).getType());
    assertEquals(TransformationType.GEOHASH, list.get(6).getType());
    assertEquals(TransformationType.JSON_MUTATE, list.get(7).getType());
    assertEquals(TransformationType.JSON_MAPPER, list.get(8).getType());
    assertEquals(TransformationType.CLOUD_EVENT_ENVELOPE, list.get(9).getType());
    assertEquals(TransformationType.CLOUD_EVENT_JSON, list.get(10).getType());
    assertEquals(TransformationType.CLOUD_EVENT_NATIVE, list.get(11).getType());

    JsonToValueTransformationDTO jsonToValue = (JsonToValueTransformationDTO) list.get(4);
    assertEquals("data.temperature", jsonToValue.getKey());

    JsonQueryTransformationDTO jsonQuery = (JsonQueryTransformationDTO) list.get(5);
    assertEquals(".", jsonQuery.getQuery());

    GeoHashResolverTransformationDTO geohash = (GeoHashResolverTransformationDTO) list.get(6);
    assertEquals("maps/location", geohash.getPrefix());
    assertEquals("latitude", geohash.getLatKey());
    assertEquals("longitude", geohash.getLonKey());
    assertEquals(7, geohash.getPrecision());
    assertEquals(GeoHashUnits.DEG, geohash.getUnits());
    assertEquals(GeoHashLayout.RAW, geohash.getLayout());
    assertEquals(GeoHashOnMissingPolicy.SKIP, geohash.getOnMissing());
    assertEquals(List.of("lat", "gps.lat"), geohash.getLatKeys());
    assertEquals(List.of("lon", "gps.lon"), geohash.getLonKeys());
  }

  @Test
  void everyTransformationTypeHasGsonMapping() {
    Gson gson = buildGson();

    for (TransformationType type : TransformationType.values()) {
      String json = """
          {"type":"%s"}
          """.formatted(type.getWireName().toLowerCase(Locale.ROOT));

      TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

      assertNotNull(dto, "Missing Gson mapping for " + type);
      assertEquals(type, dto.getType(), "Wrong DTO type value for " + type);
    }
  }

  @Test
  void unknownTypeThrows() {
    Gson gson = buildGson();

    String json = """
        {"type":"nope-not-a-real-transformer"}
        """;

    assertThrows(JsonParseException.class, () -> gson.fromJson(json, TransformationConfigDTO.class));
  }

  @Test
  void missingTypeThrows() {
    Gson gson = buildGson();

    String json = """
        {"precision":5}
        """;

    assertThrows(JsonParseException.class, () -> gson.fromJson(json, TransformationConfigDTO.class));
  }

  @Test
  void nonObjectInputThrows() {
    Gson gson = buildGson();

    assertThrows(JsonParseException.class, () -> gson.fromJson("[]", TransformationConfigDTO.class));
    assertThrows(JsonParseException.class, () -> gson.fromJson("\"geohash\"", TransformationConfigDTO.class));
    assertThrows(JsonParseException.class, () -> gson.fromJson("null", TransformationConfigDTO.class));
  }

  @Test
  void invalidEnumValueResultsInNull() {
    Gson gson = buildGson();

    String json = """
        {"type":"geohash","units":"bananas"}
        """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    GeoHashResolverTransformationDTO geohash = assertInstanceOf(GeoHashResolverTransformationDTO.class, dto);
    assertEquals(TransformationType.GEOHASH, geohash.getType());
    assertNull(geohash.getUnits());
  }

  @Test
  void defaultsAppliedWhenFieldsMissing() {
    Gson gson = buildGson();

    String json = """
        {"type":"geohash"}
        """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    GeoHashResolverTransformationDTO geohash = assertInstanceOf(GeoHashResolverTransformationDTO.class, dto);
    assertEquals(TransformationType.GEOHASH, geohash.getType());
    assertEquals(GeoHashUnits.DEG, geohash.getUnits());
    assertEquals(GeoHashLayout.CHARS_PER_SEGMENT, geohash.getLayout());
    assertEquals(GeoHashOnMissingPolicy.SKIP, geohash.getOnMissing());
  }

  @Test
  void roundTripSerializationKeepsTypeAndConcreteClass() {
    Gson gson = buildGson();

    GeoHashResolverTransformationDTO original = new GeoHashResolverTransformationDTO();
    original.setPrecision(6);
    original.setUnits(GeoHashUnits.DEG);
    original.setLayout(GeoHashLayout.RAW);
    original.setOnMissing(GeoHashOnMissingPolicy.SKIP);

    String json = gson.toJson(original);
    assertTrue(json.contains("\"type\""));

    TransformationConfigDTO parsed = gson.fromJson(json, TransformationConfigDTO.class);

    GeoHashResolverTransformationDTO geohash = assertInstanceOf(GeoHashResolverTransformationDTO.class, parsed);
    assertEquals(TransformationType.GEOHASH, geohash.getType());
    assertEquals(6, geohash.getPrecision());
    assertEquals(GeoHashUnits.DEG, geohash.getUnits());
    assertEquals(GeoHashLayout.RAW, geohash.getLayout());
    assertEquals(GeoHashOnMissingPolicy.SKIP, geohash.getOnMissing());
  }

  private static Stream<Arguments> transformationConfigurations() {
    return Stream.of(
        Arguments.of(
            "{\"type\":\"jsontoxml\"}",
            JsonToXmlTransformationDTO.class,
            TransformationType.JSON_TO_XML),

        Arguments.of(
            "{\"type\":\"xmltojson\"}",
            XmlToJsonTransformationDTO.class,
            TransformationType.XML_TO_JSON),

        Arguments.of(
            "{\"type\":\"jsontoschema\",\"schemaName\":\"example\"}",
            JsonToSchemaTransformationDTO.class,
            TransformationType.JSON_TO_SCHEMA),

        Arguments.of(
            "{\"type\":\"schematojson\"}",
            SchemaToJsonTransformationDTO.class,
            TransformationType.SCHEMA_TO_JSON),

        Arguments.of(
            "{\"type\":\"jsontovalue\",\"key\":\"data.temperature\"}",
            JsonToValueTransformationDTO.class,
            TransformationType.JSON_TO_VALUE),

        Arguments.of(
            "{\"type\":\"jsonquery\",\"query\":\".\"}",
            JsonQueryTransformationDTO.class,
            TransformationType.JSON_QUERY),

        Arguments.of(
            "{\"type\":\"geohash\",\"precision\":6,\"units\":\"deg\",\"layout\":\"raw\",\"onMissing\":\"skip\"}",
            GeoHashResolverTransformationDTO.class,
            TransformationType.GEOHASH),

        Arguments.of(
            "{\"type\":\"jsonmutate\",\"operations\":[]}",
            JsonMutateTransformationDTO.class,
            TransformationType.JSON_MUTATE),

        Arguments.of(
            "{\"type\":\"jsonmapper\",\"operations\":[]}",
            JsonMapperTransformationDTO.class,
            TransformationType.JSON_MAPPER),

        Arguments.of(
            "{\"type\":\"cloudevent-envelope\"}",
            CloudEventEnvelopeTransformationDTO.class,
            TransformationType.CLOUD_EVENT_ENVELOPE),

        Arguments.of(
            "{\"type\":\"cloudevent-json\"}",
            CloudEventJsonTransformationDTO.class,
            TransformationType.CLOUD_EVENT_JSON),

        Arguments.of(
            "{\"type\":\"cloudevent-native\"}",
            CloudEventNativeTransformationDTO.class,
            TransformationType.CLOUD_EVENT_NATIVE)
    );
  }

  private static String toMixedCase(String value) {
    StringBuilder builder = new StringBuilder();

    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);

      if (index % 2 == 0) {
        builder.append(Character.toUpperCase(character));
      }
      else {
        builder.append(Character.toLowerCase(character));
      }
    }

    return builder.toString();
  }
}