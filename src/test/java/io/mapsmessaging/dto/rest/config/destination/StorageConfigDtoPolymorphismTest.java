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

package io.mapsmessaging.dto.rest.config.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigDtoPolymorphismTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void constructors_setDeclaredDiscriminatorValues() {
    assertEquals("memory", new MemoryStorageConfigDTO().getType());
    assertEquals("partition", new PartitionStorageConfigDTO().getType());
    assertEquals("memoryTier", new MemoryTierConfigDTO().getType());
  }

  @Test
  void memory_roundTrip_preservesConcreteType() throws Exception {
    assertRoundTrip(new MemoryStorageConfigDTO(), MemoryStorageConfigDTO.class);
  }

  @Test
  void partition_roundTrip_preservesConcreteType() throws Exception {
    assertRoundTrip(new PartitionStorageConfigDTO(), PartitionStorageConfigDTO.class);
  }

  @Test
  void tieredMemory_roundTrip_preservesConcreteType() throws Exception {
    assertRoundTrip(new MemoryTierConfigDTO(), MemoryTierConfigDTO.class);
  }

  private void assertRoundTrip(StorageConfigDTO original, Class<? extends StorageConfigDTO> expectedClass) throws Exception {
    String json = objectMapper.writerFor(StorageConfigDTO.class).writeValueAsString(original);

    StorageConfigDTO parsed = objectMapper.readValue(json, StorageConfigDTO.class);

    assertInstanceOf(expectedClass, parsed);
    assertEquals(original.getType(), parsed.getType());
  }
}
