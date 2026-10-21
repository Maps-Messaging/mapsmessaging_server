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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageNameGeneratorTest {

  @Test
  void base36ParsingHandlesCaseWhitespaceAndBoundaries() {
    assertEquals(0L, MessageNameGenerator.base36ToDecimal(""));
    assertEquals(0L, MessageNameGenerator.base36ToDecimal("   "));
    assertEquals(35L, MessageNameGenerator.base36ToDecimal(" z "));
    assertEquals(36L, MessageNameGenerator.base36ToDecimal("10"));
    assertEquals(1295L, MessageNameGenerator.base36ToDecimal("ZZ"));
  }

  @Test
  void invalidBase36CharactersAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> MessageNameGenerator.base36ToDecimal("a-b"));
  }

  @Test
  void decimalFormattingUsesLowercaseBase36AndEightCharacterWidth() {
    assertEquals("       0", MessageNameGenerator.decimalToBase36(0));
    assertEquals("       z", MessageNameGenerator.decimalToBase36(35));
    assertEquals("      10", MessageNameGenerator.decimalToBase36(36));
    assertEquals("      zz", MessageNameGenerator.decimalToBase36(1295));
  }

  @Test
  void incrementProducesMonotonicallyIncreasingBase36Names() {
    String first = MessageNameGenerator.incrementString();
    String second = MessageNameGenerator.incrementString();

    assertEquals(
        MessageNameGenerator.base36ToDecimal(first) + 1,
        MessageNameGenerator.base36ToDecimal(second)
    );
  }
}
