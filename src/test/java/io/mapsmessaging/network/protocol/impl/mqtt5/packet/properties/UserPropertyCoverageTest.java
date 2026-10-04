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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class UserPropertyCoverageTest {

  @Test
  void packAndLoadRoundTripsKeyAndValue() throws Exception {
    UserProperty original = new UserProperty("region", "west");
    Packet packet = new Packet(ByteBuffer.allocate(64));

    original.pack(packet);
    packet.flip();

    UserProperty restored = (UserProperty) original.instance();
    restored.load(packet);

    assertEquals("region", restored.getUserPropertyName());
    assertEquals("west", restored.getUserPropertyValue());
  }

  @Test
  void allowsDuplicatesByProtocolDefinition() {
    assertTrue(new UserProperty("a", "b").allowDuplicates());
  }

  @Test
  void sizeIncludesBothUtf8LengthPrefixes() {
    assertEquals(10, new UserProperty("abc", "xyz").getSize());
  }

  @Test
  void settersChangeRenderedState() {
    UserProperty property = new UserProperty("a", "b");

    property.setUserPropertyName("key");
    property.setUserPropertyValue("value");

    assertEquals("key", property.getUserPropertyName());
    assertEquals("value", property.getUserPropertyValue());
    assertTrue(property.toString().contains("Key:key"));
    assertTrue(property.toString().contains("Value:value"));
  }
}
