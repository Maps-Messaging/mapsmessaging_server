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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.DestinationFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NamespaceMapTest {

  @Test
  void getOriginal_calculatesAndCachesMissingMapping() {
    DestinationFactory destinationFactory = Mockito.mock(DestinationFactory.class);
    Mockito.when(destinationFactory.calculateOriginalNamespace("tenant/_events")).thenReturn("/events");
    NamespaceMap namespaceMap = new NamespaceMap(destinationFactory);

    assertEquals("/events", namespaceMap.getOriginal("tenant/_events"));
    assertEquals("/events", namespaceMap.getOriginal("tenant/_events"));
    assertEquals("tenant/_events", namespaceMap.getMapped("/events"));
    Mockito.verify(destinationFactory).calculateOriginalNamespace("tenant/_events");
  }

  @Test
  void removeByMapped_removesBothMappingDirections() {
    DestinationFactory destinationFactory = Mockito.mock(DestinationFactory.class);
    NamespaceMap namespaceMap = new NamespaceMap(destinationFactory);
    namespaceMap.addMapped("/events", "tenant/_events");

    namespaceMap.removeByMapped("tenant/_events");

    assertNull(namespaceMap.getMapped("/events"));
    Mockito.when(destinationFactory.calculateOriginalNamespace("tenant/_events")).thenReturn("/recalculated");
    assertEquals("/recalculated", namespaceMap.getOriginal("tenant/_events"));
  }

  @Test
  void clear_removesAllMappings() {
    DestinationFactory destinationFactory = Mockito.mock(DestinationFactory.class);
    NamespaceMap namespaceMap = new NamespaceMap(destinationFactory);
    namespaceMap.addMapped("/events", "tenant/_events");
    namespaceMap.addMapped("/alerts", "tenant/_alerts");

    namespaceMap.clear();

    assertNull(namespaceMap.getMapped("/events"));
    assertNull(namespaceMap.getMapped("/alerts"));
  }
}
