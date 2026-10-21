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

import java.time.Duration;
import java.util.Objects;

public final class MavlinkDuration {

  private static final double NANOSECONDS_PER_SECOND = 1_000_000_000.0d;

  private MavlinkDuration() {
  }

  public static float toSeconds(Duration duration, String name) {
    Objects.requireNonNull(name, "name must not be null");

    if (duration == null || duration.isZero()) {
      return 0.0f;
    }
    if (duration.isNegative()) {
      throw new IllegalArgumentException(name + " must not be negative");
    }

    double seconds = duration.getSeconds() + duration.getNano() / NANOSECONDS_PER_SECOND;
    if (seconds > Float.MAX_VALUE) {
      throw new IllegalArgumentException(name + " is too large to represent as MAVLink seconds");
    }

    return (float) seconds;
  }
}