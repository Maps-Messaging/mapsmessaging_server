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

package io.mapsmessaging.engine.schema;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.schemas.config.SchemaConfig;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionUpgradeHelperTest {

  @Test
  void existingUuidReturnsRegisteredSchemaWithoutCreatingAnother() {
    SchemaManager manager = mock(SchemaManager.class);
    SchemaConfig existing = mock(SchemaConfig.class);
    String uuid = "0db54a6b-902d-49cd-b423-7d85bb69460a";

    ConfigurationProperties config = new ConfigurationProperties();
    config.put("uuid", uuid);
    when(manager.getSchema(uuid)).thenReturn(existing);

    SchemaConfig result = VersionUpgradeHelper.convertAndCreate(manager, "/topic", config);

    assertSame(existing, result);
    verify(manager, never()).addSchema(anyString(), any());
  }

  @Test
  void csvLegacyConfigurationIsConvertedAndRegistered() {
    SchemaManager manager = mock(SchemaManager.class);
    ConfigurationProperties config = base("csv");
    config.put("header", "time,value");
    config.put("numericStrings", false);
    config.put("creation", "2026-09-19T20:00:00");

    SchemaConfig result = VersionUpgradeHelper.convertAndCreate(manager, "/csv", config);

    assertEquals("csv", result.getFormat());
    assertEquals("Telemetry", result.getTitle());
    assertEquals("iface", result.getInterfaceDescription());
    assertEquals("description", result.getDescription());
    assertEquals("comments", result.getComments());
    assertEquals("monitor", result.getResourceType());
    assertEquals("topic/*", result.getMatchExpression());
    assertEquals(OffsetDateTime.parse("2026-09-19T20:00:00Z"), result.getCreatedAt());
    assertNotNull(result.getModifiedAt());
    assertNotNull(result.getSchema());
    assertEquals("time,value", result.getSchema().get("headerValues").getAsString());
    assertFalse(result.getSchema().get("interpretNumericStrings").getAsBoolean());
    verify(manager).addSchema("/csv", result);
  }

  @Test
  void protobufRequiresBothDescriptorAndMessageName() {
    SchemaManager manager = mock(SchemaManager.class);

    ConfigurationProperties complete = base("protobuf");
    complete.put("descriptor", "descriptor-data");
    complete.put("messageName", "Telemetry");
    SchemaConfig withDescriptor =
        VersionUpgradeHelper.convertAndCreate(manager, "/protobuf", complete);

    assertEquals("descriptor-data", withDescriptor.getSchema().get("descriptor").getAsString());
    assertEquals("Telemetry", withDescriptor.getSchema().get("messageName").getAsString());

    ConfigurationProperties incomplete = base("protobuf");
    incomplete.put("descriptor", "");
    incomplete.put("messageName", "Telemetry");
    SchemaConfig withoutDescriptor =
        VersionUpgradeHelper.convertAndCreate(manager, "/protobuf-empty", incomplete);

    assertNull(withoutDescriptor.getSchema());
  }

  private static ConfigurationProperties base(String format) {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("format", format);
    config.put("version", "1");
    config.put("title", "Telemetry");
    config.put("interface-description", "iface");
    config.put("description", "description");
    config.put("comments", "comments");
    config.put("uuid", java.util.UUID.randomUUID().toString());
    config.put("resource-type", "monitor");
    config.put("match-expression", "topic/*");
    return config;
  }
}
