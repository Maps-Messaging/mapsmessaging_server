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

import io.mapsmessaging.dto.rest.schema.SchemaConfigDTO;
import io.mapsmessaging.schemas.config.SchemaConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchemaConfigDtoMapper {

  private SchemaConfigDtoMapper() {
  }

  public static SchemaConfigDTO toDto(SchemaConfig schemaConfig) {
    if (schemaConfig == null) {
      return null;
    }

    SchemaConfigDTO dto = new SchemaConfigDTO();
    dto.setUniqueId(schemaConfig.getUniqueId());
    dto.setVersionId(schemaConfig.getVersionId());
    dto.setEpoch(schemaConfig.getEpoch());
    dto.setName(schemaConfig.getName());
    dto.setDescription(schemaConfig.getDescription());
    dto.setDocumentation(schemaConfig.getDocumentation());
    dto.setAncestor(schemaConfig.getAncestor());
    dto.setFormat(schemaConfig.getFormat());
    dto.setSchemaUrl(schemaConfig.getSchemaUrl());
    dto.setSchema(schemaConfig.getSchema() != null ? schemaConfig.getSchema().deepCopy() : null);
    dto.setSchemaBase64(schemaConfig.getSchemaBase64());
    dto.setCreatedAt(schemaConfig.getCreatedAt());
    dto.setModifiedAt(schemaConfig.getModifiedAt());
    dto.setNotBefore(schemaConfig.getNotBefore());
    dto.setExpiresAfter(schemaConfig.getExpiresAfter());

    Map<String, String> labels = schemaConfig.getLabels();
    if (labels != null && !labels.isEmpty()) {
      dto.setLabels(new LinkedHashMap<>(labels));
    }

    return dto;
  }

  public static SchemaConfigDTO[] toDtoArray(List<SchemaConfig> schemaConfigs) {
    if (schemaConfigs == null || schemaConfigs.isEmpty()) {
      return new SchemaConfigDTO[0];
    }

    List<SchemaConfigDTO> results = new ArrayList<>();
    for (SchemaConfig schemaConfig : schemaConfigs) {
      results.add(toDto(schemaConfig));
    }
    return results.toArray(new SchemaConfigDTO[0]);
  }

  public static List<SchemaConfigDTO> toDtoList(List<SchemaConfig> schemaConfigs) {
    if (schemaConfigs == null || schemaConfigs.isEmpty()) {
      return new ArrayList<>();
    }

    List<SchemaConfigDTO> results = new ArrayList<>();
    for (SchemaConfig schemaConfig : schemaConfigs) {
      results.add(toDto(schemaConfig));
    }
    return results;
  }
}
