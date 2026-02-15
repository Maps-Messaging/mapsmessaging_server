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
import io.mapsmessaging.dto.rest.config.transformer.impl.*;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransformationConfigDtoGsonPolymorphismTest {

  private static Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapterFactory(new TransformationConfigDtoTypeAdapterFactory())
        .create();
  }

  @Test
  void jsonMutateDeserializes() {
    Gson gson = buildGson();

    String json = """
        {"type":"json-mutate"}
        """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    assertNotNull(dto);
    assertTrue(dto instanceof JsonMutateTransformationDTO);

    JsonMutateTransformationDTO mutate = (JsonMutateTransformationDTO) dto;
    assertEquals(TransformationType.JSON_MUTATE, mutate.getType());
  }

  @Test
  void jsonMutateDiscriminatorIsCaseInsensitive() {
    Gson gson = buildGson();

    String json = """
        {"type":"JsOn-MuTaTe"}
        """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    assertNotNull(dto);
    assertTrue(dto instanceof JsonMutateTransformationDTO);

    JsonMutateTransformationDTO mutate = (JsonMutateTransformationDTO) dto;
    assertEquals(TransformationType.JSON_MUTATE, mutate.getType());
  }

  @Test
  void deserializeMixedListParsesConcreteTypes() {
    Gson gson = buildGson();
    Type listType = new TypeToken<List<TransformationConfigDTO>>() {}.getType();

    String json = """
        [
          {"type":"json-to-xml"},
          {"type":"xml-to-json"},
          {"type":"json-to-value","key":"data.temperature"},
          {"type":"json-query","query":"."},
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
          }
        ]
        """;

    List<TransformationConfigDTO> list = gson.fromJson(json, listType);

    assertNotNull(list);
    assertEquals(5, list.size());

    assertTrue(list.get(0) instanceof JsonToXmlTransformationDTO);
    assertTrue(list.get(1) instanceof XmlToJsonTransformationDTO);
    assertTrue(list.get(2) instanceof JsonToValueTransformationDTO);
    assertTrue(list.get(3) instanceof JsonQueryTransformationDTO);
    assertTrue(list.get(4) instanceof GeoHashResolverTransformationDTO);

    JsonToValueTransformationDTO jsonToValue = (JsonToValueTransformationDTO) list.get(2);
    assertEquals("data.temperature", jsonToValue.getKey());

    JsonQueryTransformationDTO jsonQuery = (JsonQueryTransformationDTO) list.get(3);
    assertEquals(".", jsonQuery.getQuery());

    GeoHashResolverTransformationDTO geohash = (GeoHashResolverTransformationDTO) list.get(4);
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
  void discriminatorIsCaseInsensitive() {
    Gson gson = buildGson();

    String json = """
        {"type":"GeOHaSh","precision":6,"units":"deg","layout":"raw","onMissing":"skip"}
        """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    assertNotNull(dto);
    assertTrue(dto instanceof GeoHashResolverTransformationDTO);

    GeoHashResolverTransformationDTO geohash = (GeoHashResolverTransformationDTO) dto;
    assertEquals(TransformationType.GEOHASH, geohash.getType());
    assertEquals(6, geohash.getPrecision());
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
  void trivialTransformersDeserialize() {
    Gson gson = buildGson();

    TransformationConfigDTO a = gson.fromJson("{\"type\":\"json-to-xml\"}", TransformationConfigDTO.class);
    TransformationConfigDTO b = gson.fromJson("{\"type\":\"xml-to-json\"}", TransformationConfigDTO.class);

    assertTrue(a instanceof JsonToXmlTransformationDTO);
    assertTrue(b instanceof XmlToJsonTransformationDTO);

    assertEquals(TransformationType.JSON_TO_XML, a.getType());
    assertEquals(TransformationType.XML_TO_JSON, b.getType());
  }

  @Test
  void invalidEnumValueResultsInNull() {
    Gson gson = buildGson();

    String json = """
    {"type":"geohash","units":"bananas"}
    """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    assertTrue(dto instanceof GeoHashResolverTransformationDTO);
    GeoHashResolverTransformationDTO geohash =
        (GeoHashResolverTransformationDTO) dto;

    assertNull(geohash.getUnits());
  }

  @Test
  void defaultsAppliedWhenFieldsMissing() {
    Gson gson = buildGson();

    String json = """
      {"type":"geohash"}
      """;

    TransformationConfigDTO dto = gson.fromJson(json, TransformationConfigDTO.class);

    assertNotNull(dto);
    assertTrue(dto instanceof GeoHashResolverTransformationDTO);

    GeoHashResolverTransformationDTO geohash = (GeoHashResolverTransformationDTO) dto;
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
    assertNotNull(parsed);
    assertTrue(parsed instanceof GeoHashResolverTransformationDTO);

    GeoHashResolverTransformationDTO geohash = (GeoHashResolverTransformationDTO) parsed;
    assertEquals(TransformationType.GEOHASH, geohash.getType());
    assertEquals(6, geohash.getPrecision());
    assertEquals(GeoHashUnits.DEG, geohash.getUnits());
    assertEquals(GeoHashLayout.RAW, geohash.getLayout());
    assertEquals(GeoHashOnMissingPolicy.SKIP, geohash.getOnMissing());
  }


}
