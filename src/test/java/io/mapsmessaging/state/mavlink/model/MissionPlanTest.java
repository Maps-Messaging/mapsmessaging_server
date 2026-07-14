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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissionPlanTest {

  @Test
  void defaultsToOneIteration() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(returnToHome()));

    assertEquals(1, missionPlan.iterations());
  }

  @Test
  void acceptsMultipleIterations() {
    MissionPlan missionPlan =
        new MissionPlan(
            List.of(returnToHome()),
            3);

    assertEquals(3, missionPlan.iterations());
  }

  @Test
  void rejectsZeroIterations() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MissionPlan(List.of(returnToHome()), 0));
  }

  @Test
  void rejectsNegativeIterations() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MissionPlan(List.of(returnToHome()), -1));
  }

  @Test
  void rejectsEmptyPlan() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MissionPlan(List.of()));
  }

  @Test
  void rejectsNullItemList() {
    assertThrows(
        NullPointerException.class,
        () -> new MissionPlan(null));
  }

  @Test
  void rejectsNullItem() {
    List<PlanItem> items = new ArrayList<>();
    items.add(returnToHome());
    items.add(null);

    assertThrows(
        NullPointerException.class,
        () -> new MissionPlan(items));
  }

  @Test
  void defensivelyCopiesItemsAndPreservesOrder() {
    PlanItem first = returnToHome();
    PlanItem second = returnToHome();
    List<PlanItem> source = new ArrayList<>(List.of(first, second));

    MissionPlan missionPlan = new MissionPlan(source);

    source.clear();

    assertEquals(2, missionPlan.items().size());
    assertSame(first, missionPlan.items().get(0));
    assertSame(second, missionPlan.items().get(1));
    assertThrows(
        UnsupportedOperationException.class,
        () -> missionPlan.items().clear());
  }

  private static PlanItem returnToHome() {
    return new PlanItem(
        PlanItemType.RETURN_TO_HOME,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
