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

package io.mapsmessaging.dto.rest.config.transformer.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.dto.rest.config.transformer.gson.TransformationConfigDtoTypeAdapterFactory;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashLayout;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashOnMissingPolicy;
import io.mapsmessaging.dto.rest.config.transformer.impl.geohash.GeoHashUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GeoHashResolverTransformationDtoDefaultsTest {

  private static Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapterFactory(new TransformationConfigDtoTypeAdapterFactory())
        .create();
  }

  @Test
  void defaultsArePresentWhenFieldsOmitted() {
    Gson gson = buildGson();

    String json = """
        {"type":"geohash"}
        """;

    GeoHashResolverTransformationDTO dto = (GeoHashResolverTransformationDTO) gson.fromJson(json, io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO.class);

    assertNotNull(dto);

    assertEquals("", dto.getPrefix());
    assertEquals("latitude", dto.getLatKey());
    assertEquals("longitude", dto.getLonKey());

    assertEquals(5, dto.getPrecision());

    assertNotNull(dto.getLatKeys());
    assertNotNull(dto.getLonKeys());
    assertTrue(dto.getLatKeys().isEmpty());
    assertTrue(dto.getLonKeys().isEmpty());

    assertEquals(GeoHashUnits.DEG, dto.getUnits());
    assertEquals(GeoHashLayout.CHARS_PER_SEGMENT, dto.getLayout());
    assertEquals(GeoHashOnMissingPolicy.SKIP, dto.getOnMissing());

    assertNull(dto.getDefaultLatitude());
    assertNull(dto.getDefaultLongitude());
  }

  @Test
  void enumValuesDeserialize() {
    Gson gson = buildGson();

    String json = """
        {
          "type":"geohash",
          "units":"e7",
          "layout":"two-per-segment",
          "onMissing":"defaultTo",
          "defaultLatitude":0.0,
          "defaultLongitude":0.0
        }
        """;

    GeoHashResolverTransformationDTO dto = (GeoHashResolverTransformationDTO) gson.fromJson(json, io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO.class);

    assertNotNull(dto);

    assertEquals(GeoHashUnits.E7, dto.getUnits());
    assertEquals(GeoHashLayout.TWO_PER_SEGMENT, dto.getLayout());
    assertEquals(GeoHashOnMissingPolicy.DEFAULT_TO, dto.getOnMissing());

    assertEquals(0.0, dto.getDefaultLatitude());
    assertEquals(0.0, dto.getDefaultLongitude());
  }
}
