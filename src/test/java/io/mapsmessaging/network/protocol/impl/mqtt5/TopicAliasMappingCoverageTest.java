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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.TopicAlias;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicAliasMappingCoverageTest {

  @Test
  void addAndLookupWorksInBothDirections() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");
    TopicAlias alias = new TopicAlias(7);

    assertTrue(mapping.add("a/b", alias));
    assertEquals("a/b", mapping.find(7));
    assertSame(alias, mapping.find("a/b"));
    assertEquals(1, mapping.size());
  }

  @Test
  void zeroAliasIsRejected() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");

    assertFalse(mapping.add("a/b", new TopicAlias(0)));
    assertEquals(0, mapping.size());
  }

  @Test
  void duplicateTopicNameIsRejected() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");

    assertTrue(mapping.add("a/b", new TopicAlias(1)));
    assertFalse(mapping.add("a/b", new TopicAlias(2)));
    assertEquals(1, mapping.size());
  }

  @Test
  void maximumPreventsAdditionalAliases() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");
    mapping.setMaximum(1);

    assertTrue(mapping.add("a", new TopicAlias(1)));
    assertFalse(mapping.add("b", new TopicAlias(2)));
  }

  @Test
  void createAllocatesNextAvailableAlias() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");

    TopicAlias first = mapping.create("first");
    TopicAlias second = mapping.create("second");

    assertNotNull(first);
    assertNotNull(second);
    assertEquals(1, first.getTopicAlias());
    assertEquals(2, second.getTopicAlias());
  }

  @Test
  void clearAllRemovesBothIndexesAndAllowsReuse() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");
    mapping.add("a", new TopicAlias(4));

    mapping.clearAll();

    assertEquals(0, mapping.size());
    assertNull(mapping.find(4));
    assertNull(mapping.find("a"));
    TopicAlias reused = mapping.create("new");
    assertEquals(0, reused.getTopicAlias());
  }

  @Test
  void unknownAliasLookupsReturnNull() {
    TopicAliasMapping mapping = new TopicAliasMapping("test");

    assertNull(mapping.find(999));
    assertNull(mapping.find("missing"));
  }
}
