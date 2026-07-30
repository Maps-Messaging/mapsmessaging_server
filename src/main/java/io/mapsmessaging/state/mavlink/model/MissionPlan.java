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

package io.mapsmessaging.state.mavlink.model;

import java.util.List;
import java.util.Objects;

public record MissionPlan(List<PlanItem> items, int iterations, boolean repeatIndefinitely) {

  public static final int REPEAT_FOREVER = -1;

  public MissionPlan(List<PlanItem> items) {
    this(items, 1, false);
  }

  public MissionPlan(List<PlanItem> items, int iterations) {
    this(items, iterations, false);
  }

  public MissionPlan {
    items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));

    if (items.isEmpty()) {
      throw new IllegalArgumentException("items must not be empty");
    }
    if (iterations < 1) {
      throw new IllegalArgumentException("iterations must be at least 1");
    }
    if (repeatIndefinitely && iterations != 1) {
      throw new IllegalArgumentException("An indefinitely repeating mission must use one logical iteration");
    }
  }

  public static MissionPlan repeatIndefinitely(List<PlanItem> items) {
    return new MissionPlan(items, 1, true);
  }

  public boolean repeats() {
    return repeatIndefinitely || iterations > 1;
  }

  public int jumpRepeatCount() {
    return repeatIndefinitely ? REPEAT_FOREVER : iterations - 1;
  }
}
