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

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MemoryStorageConfigDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

class ResourceFactoryTest {

  @TempDir
  Path tempDirectory;

  @Test
  void scanForProperties_withoutMetadataFile_returnsNull() throws IOException {
    ResourceProperties properties = ResourceFactory.getInstance().scanForProperties(tempDirectory.toFile());

    Assertions.assertNull(properties);
  }

  @Test
  void scanForProperties_afterWrite_restoresMetadataAndSchema() throws IOException {
    Date creationDate = new Date(1_725_000_000_000L);
    ResourceProperties expected = new ResourceProperties(
        creationDate,
        "orders/europe",
        "Topic",
        "123:456",
        "2026-01-02",
        "1.2.3"
    );
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("required", java.util.List.of("id"));
    expected.setSchema(schema);
    expected.setSchemaId("orders-v1");
    expected.write(tempDirectory.toFile());

    ResourceProperties actual = ResourceFactory.getInstance().scanForProperties(tempDirectory.toFile());

    Assertions.assertNotNull(actual);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  void scan_withMissingName_deletesRejectedEmptyDirectory() throws IOException {
    Path rejectedDirectory = Files.createDirectory(tempDirectory.resolve("rejected"));
    ResourceProperties properties = new ResourceProperties();
    properties.setUuid("1:2");

    Resource resource = ResourceFactory.getInstance().scan(
        new MessageExpiryHandler(),
        rejectedDirectory.toFile(),
        memoryDestinationConfig(tempDirectory),
        properties
    );

    Assertions.assertNull(resource);
    Assertions.assertFalse(Files.exists(rejectedDirectory));
  }

  @Test
  void scan_withValidProperties_buildsResourceAtUuidPath() throws IOException {
    UUID uuid = UUID.randomUUID();
    ResourceProperties properties = new ResourceProperties();
    properties.setResourceName("orders");
    properties.setUuid(uuid.getMostSignificantBits() + ":" + uuid.getLeastSignificantBits());

    Resource resource = ResourceFactory.getInstance().scan(
        new MessageExpiryHandler(),
        tempDirectory.toFile(),
        memoryDestinationConfig(tempDirectory),
        properties
    );
    try {
      String expectedName = tempDirectory + File.separator + uuid + File.separator + "message.data";
      Assertions.assertEquals(expectedName, resource.getName());
      Assertions.assertSame(properties, resource.getResourceProperties());
    } finally {
      resource.close();
    }
  }

  @Test
  void create_systemResource_isMemoryBackedAndHasNoMetadata() throws IOException {
    Resource resource = ResourceFactory.getInstance().create(
        null,
        "$SYS/health",
        null,
        null,
        null,
        null,
        null
    );
    try {
      Assertions.assertTrue(resource.getName().startsWith("Internal-Resource:"));
      Assertions.assertNull(resource.getResourceProperties());
      Assertions.assertTrue(resource.isEmpty());
    } finally {
      resource.close();
    }
  }

  private DestinationConfigDTO memoryDestinationConfig(Path directory) {
    MemoryStorageConfigDTO storageConfig = new MemoryStorageConfigDTO();
    storageConfig.setCapacity(10);
    storageConfig.setExpiredEventPoll(-1);

    DestinationConfigDTO destinationConfig = new DestinationConfigDTO();
    destinationConfig.setDirectory(directory.toString());
    destinationConfig.setStorageConfig(storageConfig);
    destinationConfig.setAutoPauseTimeout(0);
    return destinationConfig;
  }
}
