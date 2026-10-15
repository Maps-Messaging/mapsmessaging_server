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

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionSessionIdTest {
  @Test
  void identity_format_remains_stable_across_restarts_and_upgrades() {
    assertEquals("2f392cea12d28de9fe95b48", ConnectionSessionId.derive("bridge", List.of("b/#", "a/#")));
  }

  @Test
  void namespace_order_does_not_change_identity_or_mutate_input() {
    List<String> namespaces = new ArrayList<>(List.of("b/#", "a/#"));
    String id = ConnectionSessionId.derive("bridge", namespaces);
    assertEquals(id, ConnectionSessionId.derive("bridge", List.of("a/#", "b/#")));
    assertEquals(List.of("b/#", "a/#"), namespaces);
    assertTrue(id.matches("[0-9a-f]{23}"));
  }

  @Test
  void changing_namespace_or_base_changes_identity() {
    String id = ConnectionSessionId.derive("bridge", List.of("a/#"));
    assertNotEquals(id, ConnectionSessionId.derive("bridge", List.of("b/#")));
    assertNotEquals(id, ConnectionSessionId.derive("other", List.of("a/#")));
    assertNotEquals(id, ConnectionSessionId.derive("bridge", List.of()));
  }

  @Test
  void input_boundaries_are_unambiguous() {
    assertNotEquals(ConnectionSessionId.derive("bridge", List.of("ab", "c")),
        ConnectionSessionId.derive("bridge", List.of("a", "bc")));
    assertNotEquals(ConnectionSessionId.derive("a", List.of("bc")),
        ConnectionSessionId.derive("ab", List.of("c")));
    assertNotEquals(ConnectionSessionId.derive("bridge", List.of("a", "a")),
        ConnectionSessionId.derive("bridge", List.of("a")));
  }

  @Test
  void empty_and_unicode_namespaces_are_stable() {
    assertEquals(ConnectionSessionId.derive("bridge", List.of("", "温度/#")),
        ConnectionSessionId.derive("bridge", List.of("温度/#", "")));
    assertNotEquals(ConnectionSessionId.derive("bridge", List.of("温度/#")),
        ConnectionSessionId.derive("bridge", List.of("湿度/#")));
    assertNotEquals(ConnectionSessionId.derive("bridge", List.of("a/b")),
        ConnectionSessionId.derive("bridge", List.of("a//b")));
  }

  @Test
  void null_inputs_fail_fast() {
    assertThrows(NullPointerException.class, () -> ConnectionSessionId.derive(null, List.of()));
    assertThrows(NullPointerException.class, () -> ConnectionSessionId.derive("bridge", null));
    assertThrows(NullPointerException.class,
        () -> ConnectionSessionId.derive("bridge", java.util.Arrays.asList("a", null)));
  }

  @Test
  void complete_mapping_identity_covers_direction_both_paths_and_schema_namespaces() {
    List<String> initial = ConnectionSessionId.namespaces(List.of(link("push", "local/#", "remote/#", false)));
    assertFalse(initial.isEmpty());
    for (LinkConfigDTO changed : List.of(link("pull", "local/#", "remote/#", false),
        link("push", "other/#", "remote/#", false), link("push", "local/#", "other/#", false),
        link("push", "local/#", "remote/#", true))) {
      assertNotEquals(ConnectionSessionId.derive("bridge", initial),
          ConnectionSessionId.derive("bridge", ConnectionSessionId.namespaces(List.of(changed))));
    }
    assertEquals(initial, ConnectionSessionId.namespaces(List.of(link("PUSH", "local/#", "remote/#", false))));
    assertEquals(List.of(), ConnectionSessionId.namespaces(null));
  }

  private LinkConfigDTO link(String direction, String local, String remote, boolean schema) {
    LinkConfigDTO link = new LinkConfigDTO();
    link.setDirection(direction);
    link.setLocalNamespace(local);
    link.setRemoteNamespace(remote);
    link.setIncludeSchema(schema);
    return link;
  }
}
