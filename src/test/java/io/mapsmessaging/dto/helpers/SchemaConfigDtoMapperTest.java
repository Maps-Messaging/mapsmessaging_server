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

package io.mapsmessaging.dto.helpers;

import com.google.gson.JsonObject;
import io.mapsmessaging.dto.rest.schema.SchemaConfigDTO;
import io.mapsmessaging.schemas.config.SchemaConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaConfigDtoMapperTest {

  @Test
  void toDto_returnsNullForNullConfig() {
    assertNull(SchemaConfigDtoMapper.toDto(null));
  }

  @Test
  void toDto_copiesSchemaAndLabelsWithoutSharingMutableState() {
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    Map<String, String> labels = new LinkedHashMap<>();
    labels.put("environment", "test");

    SchemaConfig config = new SchemaConfig();
    config.setName("Telemetry");
    config.setSchema(schema);
    config.setLabels(labels);
    config.setUniqueId("019c21a1-0626-7258-ae03-78fd8247d4f4");

    SchemaConfigDTO result = SchemaConfigDtoMapper.toDto(config);

    assertEquals("019c21a1-0626-7258-ae03-78fd8247d4f4", result.getUniqueId());
    assertEquals("Telemetry", result.getName());
    assertEquals(schema, result.getSchema());
    assertNotSame(schema, result.getSchema());
    assertEquals(labels, result.getLabels());
    assertNotSame(labels, result.getLabels());

    schema.addProperty("changed", true);
    labels.put("owner", "operations");
    assertFalse(result.getSchema().has("changed"));
    assertFalse(result.getLabels().containsKey("owner"));
  }

  @Test
  void toDtoArray_returnsEmptyArrayForNullAndEmptyInputs() {
    assertArrayEquals(new SchemaConfigDTO[0], SchemaConfigDtoMapper.toDtoArray(null));
    assertArrayEquals(new SchemaConfigDTO[0], SchemaConfigDtoMapper.toDtoArray(List.of()));
  }

  @Test
  void toDtoList_returnsIndependentMutableEmptyLists() {
    List<SchemaConfigDTO> first = SchemaConfigDtoMapper.toDtoList(null);
    List<SchemaConfigDTO> second = SchemaConfigDtoMapper.toDtoList(List.of());

    first.add(new SchemaConfigDTO());

    assertEquals(1, first.size());
    assertTrue(second.isEmpty());
  }

  @Test
  void collectionMappings_preserveOrderAndNullEntries() {
    SchemaConfig first = new SchemaConfig();
    first.setUniqueId("019c21a1-0626-7258-ae03-78fd8247d4f4");
    SchemaConfig second = new SchemaConfig();
    second.setUniqueId("019c21a1-0626-7258-ae03-78fd8247d4f5");
    List<SchemaConfig> source = new ArrayList<>(List.of(first, second));
    source.add(1, null);

    SchemaConfigDTO[] arrayResult = SchemaConfigDtoMapper.toDtoArray(source);
    List<SchemaConfigDTO> listResult = SchemaConfigDtoMapper.toDtoList(source);

    assertEquals("019c21a1-0626-7258-ae03-78fd8247d4f4", arrayResult[0].getUniqueId());
    assertNull(arrayResult[1]);
    assertEquals("019c21a1-0626-7258-ae03-78fd8247d4f5", arrayResult[2].getUniqueId());
    assertEquals(List.of(
            "019c21a1-0626-7258-ae03-78fd8247d4f4",
            "019c21a1-0626-7258-ae03-78fd8247d4f5"),
        listResult.stream().filter(dto -> dto != null).map(SchemaConfigDTO::getUniqueId).toList());
    assertNull(listResult.get(1));
  }
}
