/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The NamespaceMap class is responsible for mapping original namespaces to mapped namespaces and vice versa.
 * It provides methods to add, remove, and retrieve mappings between original and mapped namespaces.
 * The class also supports clearing all mappings.
 *
 * The NamespaceMap class uses two LinkedHashMaps to store the mappings:
 * - originalToMapped: stores the mapping from original namespaces to mapped namespaces
 * - mappedToOriginal: stores the mapping from mapped namespaces to original namespaces
 *
 * The class provides the following methods:
 * - clear(): Clears all mappings in the NamespaceMap.
 * - addMapped(original, mapped): Adds a mapping between an original namespace and a mapped namespace.
 * - getMapped(original): Retrieves the mapped namespace for a given original namespace.
 * - getOriginal(mapped): Retrieves the original namespace for a given mapped namespace.
 * - removeByMapped(fullyQualifiedNamespace): Removes the mapping for a given fully qualified namespace.
 *
 * Usage example:
 * ```
 * NamespaceMap namespaceMap = new NamespaceMap();
 * namespaceMap.addMapped("originalNamespace", "mappedNamespace");
 * String mapped = namespaceMap.getMapped("originalNamespace"); // returns "mappedNamespace"
 * String original = namespaceMap.getOriginal("mappedNamespace"); // returns "originalNamespace"
 * namespaceMap.removeByMapped("mappedNamespace");
 * ```
 */
public class NamespaceMap {

  private final Map<String, String> originalToMapped;
  private final Map<String, String> mappedToOriginal;
  private final DestinationFactory destinationManager;

  public NamespaceMap(DestinationFactory destinationManager) {
    this.destinationManager = destinationManager;
    originalToMapped = new LinkedHashMap<>();
    mappedToOriginal = new LinkedHashMap<>();
  }

  public void clear() {
    originalToMapped.clear();
    mappedToOriginal.clear();
  }

  public void addMapped(String original, String mapped) {
    originalToMapped.put(original, mapped);
    mappedToOriginal.put(mapped, original);
  }

  public String getMapped(String original) {
    return originalToMapped.get(original);
  }

  public String getOriginal(String mapped) {
    String located = mappedToOriginal.get(mapped);
    if (located == null) {
      located = destinationManager.calculateOriginalNamespace(mapped);
      addMapped(located, mapped);
    }
    return located;
  }

  public void removeByMapped(String fullyQualifiedNamespace) {
    String found = mappedToOriginal.remove(fullyQualifiedNamespace);
    if (found != null) {
      originalToMapped.remove(found);
    }
  }
}
