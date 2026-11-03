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

package io.mapsmessaging.network.route;

import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkId;
import io.mapsmessaging.network.route.link.LinkMetrics;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.network.route.select.CostWeights;
import io.mapsmessaging.network.route.select.DefaultCostFunction;
import io.mapsmessaging.network.route.select.LinkSelector;
import io.mapsmessaging.network.route.select.LinkSwitcher;
import io.mapsmessaging.network.route.select.SelectionPolicy;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LinkSelectorReliabilityTest {

  @ParameterizedTest(name = "selected A={0}, standby={1}, {2}={3}, hard failover={4}")
  @MethodSource("healthBoundaries")
  void current_link_hard_limit_bypasses_cooldown_only_at_or_above_threshold(boolean selectedIsPrimary, LinkState standbyState,
      HealthMetric metric, double value, boolean hardFailover) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.CONNECTED, HealthMetric.LATENCY, 100, 10);
      TestLink standby = link(selectedIsPrimary ? "B" : "A", standbyState, HealthMetric.LATENCY, 100, 10);
      RecordingSwitcher switcher = new RecordingSwitcher(null);
      LinkSelector selector = selector(switcher, List.of(selected, standby));
      selector.evaluateOnce();
      assertSame(selected, switcher.getCurrentLink());
      now.set(started.plusSeconds(3));
      setMetric(selected.metrics(), metric, value);
      assertEquals(hardFailover, selector.evaluateOnce().switched(), "Only hard failure bypasses the switch cooldown");
      assertSame(hardFailover ? standby : selected, switcher.getCurrentLink());
    }
  }

  @ParameterizedTest(name = "selected A={0}, standby={1}, limit={2}, healthy base={3}, standby base={4}")
  @MethodSource("healthCostCases")
  void failover_orders_destinations_using_computed_health_and_realistic_base_costs(boolean selectedIsPrimary,
      LinkState standbyState, HealthMetric metric, int healthyBaseCost, int standbyBaseCost) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.DISCONNECTED, HealthMetric.LATENCY, 100, 10);
      TestLink unreliable = link(selectedIsPrimary ? "B" : "A", standbyState, metric, metric.limit(), standbyBaseCost);
      TestLink reliable = link("C", standbyState, HealthMetric.LATENCY, 100, healthyBaseCost);
      RecordingSwitcher switcher = new RecordingSwitcher(selected);
      LinkSelector selector = selector(switcher, List.of(selected, unreliable, reliable));
      selector.evaluateOnce();
      now.set(started.plusSeconds(3));
      selector.evaluateOnce();
      assertSame(preferHealthy(metric, healthyBaseCost, standbyBaseCost) ? reliable : unreliable, switcher.getCurrentLink(),
          "Selection must match the independently calculated weighted score");
    }
  }

  @ParameterizedTest(name = "selected A={0}, standby={1}, limit={2}, healthy base={3}, standby base={4}")
  @MethodSource("healthCostCases")
  void computed_cost_and_hysteresis_preserve_the_healthy_current_link(boolean selectedIsPrimary,
      LinkState standbyState, HealthMetric metric, int healthyBaseCost, int standbyBaseCost) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.CONNECTED, HealthMetric.LATENCY, 100, healthyBaseCost);
      TestLink unreliable = link(selectedIsPrimary ? "B" : "A", standbyState, metric, metric.limit(), standbyBaseCost);
      RecordingSwitcher switcher = new RecordingSwitcher(selected);
      LinkSelector selector = selector(switcher, List.of(selected, unreliable));
      selector.evaluateOnce();
      now.set(started.plusSeconds(3));
      selector.evaluateOnce();
      // The only lower standby score in this matrix is 170 versus 200 (error, bases 10/100).
      // That is exactly 15% improvement; the strict hysteresis comparison must retain the current link.
      assertSame(selected, switcher.getCurrentLink(), "Computed health cost and hysteresis must both govern switching");
    }
  }

  @ParameterizedTest(name = "selected A={0}, standby={1}, transient={2}")
  @MethodSource("unhealthyStandbys")
  void transient_cost_increase_during_cooldown_does_not_cause_switch_and_switch_back(boolean selectedIsPrimary,
      LinkState standbyState, HealthMetric metric) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.CONNECTED, HealthMetric.LATENCY, 100, 10);
      TestLink standby = link(selectedIsPrimary ? "B" : "A", standbyState, HealthMetric.LATENCY, 100, 10);
      RecordingSwitcher switcher = new RecordingSwitcher(null);
      LinkSelector selector = selector(switcher, List.of(selected, standby));
      selector.evaluateOnce();
      assertSame(selected, switcher.getCurrentLink());
      now.set(started.plusSeconds(3));
      double degraded = switch (metric) {
        case LATENCY -> 1999.0;
        case LOSS -> 0.249;
        case ERROR -> 0.199;
      };
      setMetric(selected.metrics(), metric, degraded);
      assertFalse(selector.evaluateOnce().switched(), "A transient below the hard limit must respect cooldown");
      now.set(started.plusSeconds(4));
      setMetric(selected.metrics(), metric, metric == HealthMetric.LATENCY ? 100.0 : 0.0);
      assertFalse(selector.evaluateOnce().switched());
      now.set(started.plusSeconds(10));
      assertFalse(selector.evaluateOnce().switched());
      now.set(started.plusSeconds(301));
      assertFalse(selector.evaluateOnce().switched(), "Recovery to equal cost must not cause a delayed switch");
      assertSame(selected, switcher.getCurrentLink());
    }
  }

  private static boolean preferHealthy(HealthMetric metric, int healthyBaseCost, int standbyBaseCost) {
    // Hand-calculated default-weight costs: 100 ms healthy; 2000 ms latency limit;
    // 100 ms + 400 * 0.25 loss; or 100 ms + 300 * 0.20 error. Other penalties are zero.
    double degradedHealthCost = switch (metric) {
      case LATENCY -> 2000.0;
      case LOSS -> 200.0;
      case ERROR -> 160.0;
    };
    return 100.0 + healthyBaseCost <= degradedHealthCost + standbyBaseCost;
  }

  private static void setMetric(LinkMetrics metrics, HealthMetric dimension, double value) {
    switch (dimension) {
      case LATENCY -> when(metrics.getLatencyMillisEma()).thenReturn(OptionalDouble.of(value));
      case LOSS -> when(metrics.getLossRatio()).thenReturn(value);
      case ERROR -> when(metrics.getErrorRate()).thenReturn(value);
    }
  }

  private static Stream<Arguments> healthBoundaries() {
    List<Arguments> cases = new ArrayList<>();
    for (boolean selectedIsPrimary : new boolean[] {true, false}) {
      for (LinkState state : List.of(LinkState.HOLDING, LinkState.CONNECTED)) {
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LATENCY, 1999.0, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LATENCY, 2000.0, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LATENCY, 2001.0, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LOSS, 0.249, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LOSS, 0.25, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LOSS, 0.251, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.ERROR, 0.199, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.ERROR, 0.20, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.ERROR, 0.201, true));
      }
    }
    return cases.stream();
  }

  private static Stream<Arguments> healthCostCases() {
    List<Arguments> cases = new ArrayList<>();
    int[][] baseCosts = {{10, 10}, {10, 50}, {10, 100}, {50, 10}, {50, 50}, {50, 100}, {100, 10}, {100, 50}, {100, 100}};
    for (boolean selectedIsPrimary : new boolean[] {true, false}) {
      for (LinkState state : List.of(LinkState.HOLDING, LinkState.CONNECTED)) {
        for (HealthMetric metric : HealthMetric.values()) {
          for (int[] costs : baseCosts) {
            cases.add(Arguments.of(selectedIsPrimary, state, metric, costs[0], costs[1]));
          }
        }
      }
    }
    return cases.stream();
  }

  private static Stream<Arguments> unhealthyStandbys() {
    List<Arguments> cases = new ArrayList<>();
    for (boolean selectedIsPrimary : new boolean[] {true, false}) {
      for (LinkState state : List.of(LinkState.HOLDING, LinkState.CONNECTED)) {
        for (HealthMetric metric : HealthMetric.values()) {
          cases.add(Arguments.of(selectedIsPrimary, state, metric));
        }
      }
    }
    return cases.stream();
  }

  private static LinkSelector selector(RecordingSwitcher switcher, List<Link> links) {
    return LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(SelectionPolicy.builder().establishmentWarmup(Duration.ofSeconds(3)).build())
        .linkRepository(() -> links)
        .linkSwitcher(switcher)
        .build();
  }

  private static TestLink link(String name, LinkState state, HealthMetric dimension, double value, int baseCost) {
    LinkMetrics metrics = mock(LinkMetrics.class);
    when(metrics.getLatencyMillisEma()).thenReturn(OptionalDouble.of(dimension == HealthMetric.LATENCY ? value : 100.0));
    when(metrics.getJitterMillisEma()).thenReturn(OptionalDouble.of(0));
    when(metrics.getLossRatio()).thenReturn(dimension == HealthMetric.LOSS ? value : 0.0);
    when(metrics.getErrorRate()).thenReturn(dimension == HealthMetric.ERROR ? value : 0.0);
    when(metrics.getLastUpdated()).thenAnswer(invocation -> Instant.now());
    when(metrics.getWindow()).thenReturn(Duration.ofSeconds(5));
    return new TestLink(new LinkId(name), state, metrics, baseCost);
  }

  private enum HealthMetric {
    LATENCY, LOSS, ERROR;

    private double limit() {
      return switch (this) {
        case LATENCY -> 2000.0;
        case LOSS -> 0.25;
        case ERROR -> 0.20;
      };
    }
  }

  private record TestLink(LinkId id, LinkState state, LinkMetrics metrics, int cost) implements Link {
    @Override
    public LinkId getLinkId() {
      return id;
    }

    @Override
    public URI getRemoteUri() {
      return URI.create("test://" + id.value());
    }

    @Override
    public LinkState getState() {
      return state;
    }

    @Override
    public LinkMetrics getMetrics() {
      return metrics;
    }

    @Override
    public OptionalDouble getBaseCost() {
      return OptionalDouble.of(cost);
    }

    @Override
    public boolean isAvailable() {
      return state == LinkState.CONNECTED || state == LinkState.HOLDING;
    }

    @Override
    public void connect() {
      throw new UnsupportedOperationException("Transport is outside this selector test");
    }

    @Override
    public void disconnect() {
      throw new UnsupportedOperationException("Transport is outside this selector test");
    }

  }

  private static final class RecordingSwitcher implements LinkSwitcher {
    private Link current;

    private RecordingSwitcher(Link initial) {
      current = initial;
    }

    @Override
    public Link getCurrentLink() {
      return current;
    }

    @Override
    public boolean switchTo(Link next, String reason) {
      if (next == current) {
        return false;
      }
      current = next;
      return true;
    }
  }
}
