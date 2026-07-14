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

package io.mapsmessaging.state.mavlink.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MavlinkDurationTest {

  @Test
  void nullAndZeroDurationConvertToZeroSeconds() {
    assertEquals(0.0f, MavlinkDuration.toSeconds(null, "duration"));
    assertEquals(0.0f, MavlinkDuration.toSeconds(Duration.ZERO, "duration"));
  }

  @Test
  void fractionalSecondsArePreserved() {
    assertEquals(0.25f, MavlinkDuration.toSeconds(Duration.ofMillis(250), "duration"));
    assertEquals(1.5f, MavlinkDuration.toSeconds(Duration.ofMillis(1500), "duration"));
    assertEquals(10.125f, MavlinkDuration.toSeconds(Duration.ofMillis(10_125), "duration"));
  }

  @Test
  void negativeDurationIsRejected() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> MavlinkDuration.toSeconds(Duration.ofNanos(-1), "holdDuration"));

    assertEquals("holdDuration must not be negative", exception.getMessage());
  }

  @Test
  void nullFieldNameIsRejected() {
    assertThrows(
        NullPointerException.class,
        () -> MavlinkDuration.toSeconds(Duration.ofSeconds(1), null));
  }
}
