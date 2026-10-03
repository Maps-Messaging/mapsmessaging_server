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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ContentType;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.UserProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessagePropertiesCoverageTest {

  @Test
  void firstSingletonPropertyIsNotDuplicate() {
    MessageProperties properties = new MessageProperties();

    properties.add(new ContentType("text/plain"));

    assertTrue(properties.duplicates().isEmpty());
    assertEquals("text/plain",
        ((ContentType) properties.get(new ContentType("x").getId())).getContentType());
  }

  @Test
  void repeatedSingletonPropertyIsReportedAsDuplicate() {
    MessageProperties properties = new MessageProperties();
    ContentType first = new ContentType("a");
    ContentType second = new ContentType("b");

    properties.add(first).add(second);

    assertEquals(1, properties.duplicates().size());
    assertSame(second, properties.duplicates().get(0));
    assertTrue(properties.getDuplicateReport().contains("ContentType"));
  }

  @Test
  void userPropertiesMayRepeatWithoutDuplicateReport() {
    MessageProperties properties = new MessageProperties();

    properties.add(new UserProperty("a", "1"));
    properties.add(new UserProperty("b", "2"));

    assertTrue(properties.duplicates().isEmpty());
    assertEquals(2, properties.values().size());
  }

  @Test
  void subscriptionIdentifiersMayRepeatWithoutDuplicateReport() {
    MessageProperties properties = new MessageProperties();

    properties.add(new SubscriptionIdentifier(1));
    properties.add(new SubscriptionIdentifier(2));

    assertTrue(properties.duplicates().isEmpty());
    assertEquals(2, properties.values().size());
  }

  @Test
  void missingPropertyLookupReturnsNull() {
    assertNull(new MessageProperties().get(0x7F));
  }

  @Test
  void removeByPropertyRemovesAllInstancesWithSameIdentifier() {
    MessageProperties properties = new MessageProperties();
    ContentType first = new ContentType("a");
    ContentType second = new ContentType("b");
    properties.add(first).add(second);

    properties.remove(first);

    assertNull(properties.get(first.getId()));
    assertTrue(properties.values().isEmpty());
  }

  @Test
  void removeByIdentifierRemovesOneMatchingProperty() {
    MessageProperties properties = new MessageProperties();
    UserProperty first = new UserProperty("a", "1");
    UserProperty second = new UserProperty("b", "2");
    properties.add(first).add(second);

    properties.remove(first.getId());

    assertEquals(1, properties.values().size());
    assertSame(second, properties.values().iterator().next());
  }

  @Test
  void toStringContainsStoredProperties() {
    MessageProperties properties = new MessageProperties();
    properties.add(new ContentType("application/json"));

    String rendered = properties.toString();

    assertTrue(rendered.startsWith("Properties > "));
    assertTrue(rendered.contains("ContentType"));
  }
}
