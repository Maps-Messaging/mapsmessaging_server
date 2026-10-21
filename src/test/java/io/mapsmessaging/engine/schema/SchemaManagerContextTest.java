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

import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SchemaManagerContextTest {
  @Test
  void resource_id_fallback_returns_the_resource_when_no_title_matches() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaResource resource = mock(SchemaResource.class);
    when(repository.getAllSchemas()).thenReturn(List.of());
    when(repository.getResource("schema-id")).thenReturn(resource);
    SchemaManager manager = mock(SchemaManager.class, CALLS_REAL_METHODS);
    Field repositoryField = SchemaManager.class.getDeclaredField("repository");
    repositoryField.setAccessible(true);
    repositoryField.set(manager, repository);

    assertEquals(List.of(resource), manager.getSchemaByContext("schema-id"));
  }
}
