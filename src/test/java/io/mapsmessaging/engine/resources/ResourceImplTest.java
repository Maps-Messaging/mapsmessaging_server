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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class ResourceImplTest {

  @Test
  void newInternalResource_isEmptyAndGeneratesSequentialIdentifiers() throws IOException {
    ResourceImpl resource = new ResourceImpl();
    try {
      Assertions.assertTrue(resource.isEmpty());
      Assertions.assertEquals(0L, resource.size());
      Assertions.assertTrue(resource.getKeys().isEmpty());
      Assertions.assertEquals(1L, resource.getNextIdentifier());
      Assertions.assertEquals(2L, resource.getNextIdentifier());
    } finally {
      resource.close();
    }
  }

  @Test
  void missingIdentifier_returnsNoMessageAndIsNotContained() throws IOException {
    ResourceImpl resource = new ResourceImpl();
    try {
      Assertions.assertFalse(resource.contains(99L));
      Assertions.assertNull(resource.get(99L));
    } finally {
      resource.close();
    }
  }

  @Test
  void keepOnly_emptyList_onEmptyResource_preservesEmptyState() throws IOException {
    ResourceImpl resource = new ResourceImpl();
    try {
      resource.keepOnly(List.of());

      Assertions.assertTrue(resource.isEmpty());
      Assertions.assertEquals(0L, resource.size());
    } finally {
      resource.close();
    }
  }

  @Test
  void close_canBeCalledMoreThanOnce() throws IOException {
    ResourceImpl resource = new ResourceImpl();

    resource.close();
    resource.close();
  }
}
