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

  @ParameterizedTest(name = "selected A={0}, standby={1}, {2}={3}, eligible={4}")
  @MethodSource("healthBoundaries")
  void failover_rejects_standby_at_or_above_hard_health_limit(boolean selectedIsPrimary, LinkState standbyState,
      HealthMetric metric, double value, boolean eligible) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.DISCONNECTED, HealthMetric.LATENCY, 100, 0);
      TestLink standby = link(selectedIsPrimary ? "B" : "A", standbyState, metric, value, 0);
      RecordingSwitcher switcher = new RecordingSwitcher(selected);
      LinkSelector selector = selector(switcher, List.of(selected, standby));
      if (standbyState == LinkState.HOLDING) {
        assertFalse(selector.evaluateOnce().switched(), "Holding link must first complete warmup");
      }
      now.set(started.plusSeconds(3));
      assertEquals(eligible, selector.evaluateOnce().switched(), "Hard health limits must apply to the destination link");
      assertSame(eligible ? standby : selected, switcher.getCurrentLink());
    }
  }

  @ParameterizedTest(name = "selected A={0}, unreliable standby={1}, limit={2}")
  @MethodSource("unhealthyStandbys")
  void failover_prefers_reliable_alternative_even_when_unreliable_link_has_lower_cost(boolean selectedIsPrimary,
      LinkState standbyState, HealthMetric metric) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.DISCONNECTED, HealthMetric.LATENCY, 100, 0);
      TestLink unreliable = link(selectedIsPrimary ? "B" : "A", standbyState, metric, metric.limit(), 0);
      TestLink reliable = link("C", LinkState.HOLDING, HealthMetric.LATENCY, 100, 50000);
      RecordingSwitcher switcher = new RecordingSwitcher(selected);
      LinkSelector selector = selector(switcher, List.of(selected, unreliable, reliable));
      selector.evaluateOnce();
      now.set(started.plusSeconds(3));
      selector.evaluateOnce();
      assertSame(reliable, switcher.getCurrentLink(), "An unreliable destination must not win on cost");
    }
  }

  @ParameterizedTest(name = "selected A={0}, cheaper standby={1}, limit={2}")
  @MethodSource("unhealthyStandbys")
  void reliable_current_link_is_not_replaced_by_cheaper_unreliable_standby(boolean selectedIsPrimary,
      LinkState standbyState, HealthMetric metric) {
    Instant started = Instant.parse("2026-11-03T00:00:00Z");
    AtomicReference<Instant> now = new AtomicReference<>(started);
    try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenAnswer(invocation -> now.get());
      TestLink selected = link(selectedIsPrimary ? "A" : "B", LinkState.CONNECTED, HealthMetric.LATENCY, 100, 50000);
      TestLink unreliable = link(selectedIsPrimary ? "B" : "A", standbyState, metric, metric.limit(), 0);
      RecordingSwitcher switcher = new RecordingSwitcher(selected);
      LinkSelector selector = selector(switcher, List.of(selected, unreliable));
      selector.evaluateOnce();
      now.set(started.plusSeconds(3));
      selector.evaluateOnce();
      assertSame(selected, switcher.getCurrentLink(), "Healthy current link must not be replaced by an unreliable cheaper link");
    }
  }

  private static Stream<Arguments> healthBoundaries() {
    List<Arguments> cases = new ArrayList<>();
    for (boolean selectedIsPrimary : new boolean[] {true, false}) {
      for (LinkState state : List.of(LinkState.HOLDING, LinkState.CONNECTED)) {
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LATENCY, 1999.0, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LATENCY, 2000.0, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LATENCY, 2001.0, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LOSS, 0.249, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LOSS, 0.25, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.LOSS, 0.251, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.ERROR, 0.199, true));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.ERROR, 0.20, false));
        cases.add(Arguments.of(selectedIsPrimary, state, HealthMetric.ERROR, 0.201, false));
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
        .selectionPolicy(SelectionPolicy.builder().establishmentWarmup(Duration.ofSeconds(3))
            .minimumHoldTime(Duration.ZERO).cooldownAfterSwitch(Duration.ZERO).build())
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
