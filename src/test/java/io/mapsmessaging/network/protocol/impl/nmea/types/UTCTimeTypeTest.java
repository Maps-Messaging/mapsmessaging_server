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

package io.mapsmessaging.network.protocol.impl.nmea.types;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class UTCTimeTypeTest {

  @Test
  void parsesWholeSecondUtcTime() {
    UTCTimeType time = new UTCTimeType("123519");

    assertEquals(12, time.getTime().getHour());
    assertEquals(35, time.getTime().getMinute());
    assertEquals(19, time.getTime().getSecond());
    assertEquals(ZoneOffset.UTC, time.getTime().getOffset());
    assertEquals(time.getTime().toString(), time.toString());
    assertEquals(time.toString(), time.jsonPack());
  }

  @Test
  void parsesFractionalSecondsToMilliseconds() {
    UTCTimeType time = new UTCTimeType("000001.250");

    assertEquals(1, time.getTime().getSecond());
    assertEquals(250_000_000, time.getTime().getNano());
  }

  @Test
  void invalidClockValuesAreRejected() {
    assertThrows(RuntimeException.class, () -> new UTCTimeType("250000"));
    assertThrows(NumberFormatException.class, () -> new UTCTimeType("not-time"));
  }
}
