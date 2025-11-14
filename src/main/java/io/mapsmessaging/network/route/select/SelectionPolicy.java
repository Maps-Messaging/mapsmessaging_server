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

package io.mapsmessaging.network.route.select;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder(toBuilder = true)
public class SelectionPolicy {
  @Builder.Default
  private final double hysteresisRatio = 0.15;
  @Builder.Default
  private final Duration minimumHoldTime = Duration.ofSeconds(10);
  @Builder.Default
  private final Duration cooldownAfterSwitch = Duration.ofSeconds(10);
  @Builder.Default
  private final double hardMaxLatencyMillis = 2_000.0;
  @Builder.Default
  private final double hardMaxLossRatio = 0.25;
  @Builder.Default
  private final double hardMaxErrorRate = 0.20;
  @Builder.Default
  private final int maxSwitchesPerWindow = 1;
  @Builder.Default
  private final Duration flapWindow = Duration.ofMinutes(5);
  @Builder.Default
  private final Duration minInterEventInterval = Duration.ofMillis(75);
  @Builder.Default
  private final double tieBreakEpsilon = 0.0;
  @Builder.Default
  private final Duration establishmentWarmup = Duration.ofSeconds(5);
}
