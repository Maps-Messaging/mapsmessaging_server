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

package io.mapsmessaging.rest.api.impl.destination.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NamespaceNormalizerTest {

  @Test
  void normalize_maps_null_and_whitespace_to_empty_path() {
    assertEquals("", NamespaceNormalizer.normalize(null));
    assertEquals("", NamespaceNormalizer.normalize(""));
    assertEquals("", NamespaceNormalizer.normalize(" \t\r\n "));
  }

  @Test
  void normalize_collapses_repeated_slashes_and_removes_trailing_slash() {
    assertEquals("sensors/room/temperature", NamespaceNormalizer.normalize("  sensors//room///temperature/  "));
    assertEquals("/sensors/room/temperature", NamespaceNormalizer.normalize("  ///sensors//room///temperature///  "));
  }

  @Test
  void normalize_preserves_absolute_root_marker() {
    assertEquals("/", NamespaceNormalizer.normalize("/"));
    assertEquals("/", NamespaceNormalizer.normalize("////"));
  }

  @Test
  void split_normalized_preserves_absolute_marker_and_segments() {
    assertArrayEquals(new String[]{"", "sensors", "room"}, NamespaceNormalizer.splitNormalized("/sensors/room"));
    assertArrayEquals(new String[]{"sensors", "room"}, NamespaceNormalizer.splitNormalized("sensors/room"));
  }

  @Test
  void split_normalized_handles_empty_and_absolute_root_paths() {
    assertArrayEquals(new String[0], NamespaceNormalizer.splitNormalized(null));
    assertArrayEquals(new String[0], NamespaceNormalizer.splitNormalized(""));
    assertArrayEquals(new String[]{""}, NamespaceNormalizer.splitNormalized("/"));
  }
}
