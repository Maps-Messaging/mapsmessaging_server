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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UxvNavigationPlanTest {

  @Test
  void constructorStoresBothPhasesDurationAndTerminalAction() {
    UxvModelCommandSet mission = mock(UxvModelCommandSet.class);
    UxvModelCommandSet start = mock(UxvModelCommandSet.class);
    UxvModelCommandSet terminal = mock(UxvModelCommandSet.class);
    Duration duration = Duration.ofMinutes(5);

    UxvNavigationPlan plan = new UxvNavigationPlan(List.of(mission), List.of(start), duration, terminal);

    assertEquals(List.of(mission), plan.missionPhase());
    assertEquals(List.of(start), plan.postMissionUploadPhase());
    assertEquals(duration, plan.duration());
    assertSame(terminal, plan.terminalAction());
    assertTrue(plan.hasTimeout());
  }

  @Test
  void nullDurationNormalisesToZero() {
    UxvNavigationPlan plan = new UxvNavigationPlan(
        List.of(mock(UxvModelCommandSet.class)),
        List.of(mock(UxvModelCommandSet.class)),
        null,
        mock(UxvModelCommandSet.class));

    assertEquals(Duration.ZERO, plan.duration());
    assertFalse(plan.hasTimeout());
  }

  @Test
  void zeroDurationHasNoTimeout() {
    UxvNavigationPlan plan = new UxvNavigationPlan(
        List.of(mock(UxvModelCommandSet.class)),
        List.of(mock(UxvModelCommandSet.class)),
        Duration.ZERO,
        mock(UxvModelCommandSet.class));

    assertFalse(plan.hasTimeout());
  }

  @Test
  void constructorDefensivelyCopiesBothPhases() {
    UxvModelCommandSet mission = mock(UxvModelCommandSet.class);
    UxvModelCommandSet start = mock(UxvModelCommandSet.class);
    List<UxvModelCommandSet> missionPhase = new ArrayList<>(List.of(mission));
    List<UxvModelCommandSet> postMissionUploadPhase = new ArrayList<>(List.of(start));

    UxvNavigationPlan plan = new UxvNavigationPlan(
        missionPhase,
        postMissionUploadPhase,
        Duration.ZERO,
        mock(UxvModelCommandSet.class));

    missionPhase.clear();
    postMissionUploadPhase.clear();

    assertEquals(List.of(mission), plan.missionPhase());
    assertEquals(List.of(start), plan.postMissionUploadPhase());
    assertThrows(UnsupportedOperationException.class, () -> plan.missionPhase().clear());
    assertThrows(UnsupportedOperationException.class, () -> plan.postMissionUploadPhase().clear());
  }

  @Test
  void constructorRejectsNullMissionPhase() {
    assertThrows(
        NullPointerException.class,
        () -> new UxvNavigationPlan(
            null,
            List.of(mock(UxvModelCommandSet.class)),
            Duration.ZERO,
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsEmptyMissionPhase() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new UxvNavigationPlan(
            List.of(),
            List.of(mock(UxvModelCommandSet.class)),
            Duration.ZERO,
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsNullMissionPhaseEntry() {
    List<UxvModelCommandSet> missionPhase = new ArrayList<>();
    missionPhase.add(null);

    assertThrows(
        NullPointerException.class,
        () -> new UxvNavigationPlan(
            missionPhase,
            List.of(mock(UxvModelCommandSet.class)),
            Duration.ZERO,
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsNullPostMissionUploadPhase() {
    assertThrows(
        NullPointerException.class,
        () -> new UxvNavigationPlan(
            List.of(mock(UxvModelCommandSet.class)),
            null,
            Duration.ZERO,
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsEmptyPostMissionUploadPhase() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new UxvNavigationPlan(
            List.of(mock(UxvModelCommandSet.class)),
            List.of(),
            Duration.ZERO,
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsNullPostMissionUploadPhaseEntry() {
    List<UxvModelCommandSet> postMissionUploadPhase = new ArrayList<>();
    postMissionUploadPhase.add(null);

    assertThrows(
        NullPointerException.class,
        () -> new UxvNavigationPlan(
            List.of(mock(UxvModelCommandSet.class)),
            postMissionUploadPhase,
            Duration.ZERO,
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsNegativeDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new UxvNavigationPlan(
            List.of(mock(UxvModelCommandSet.class)),
            List.of(mock(UxvModelCommandSet.class)),
            Duration.ofSeconds(-1),
            mock(UxvModelCommandSet.class)));
  }

  @Test
  void constructorRejectsNullTerminalAction() {
    assertThrows(
        NullPointerException.class,
        () -> new UxvNavigationPlan(
            List.of(mock(UxvModelCommandSet.class)),
            List.of(mock(UxvModelCommandSet.class)),
            Duration.ZERO,
            null));
  }
}