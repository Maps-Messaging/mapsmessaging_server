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
package io.mapsmessaging.network.route;

import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkId;
import io.mapsmessaging.network.route.link.LinkMetrics;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.network.route.select.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LinkSelectorEdgeCasesTest {

  // ---------- Selector edge cases ----------

  @Test
  void tie_break_holds_current() {
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.10)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(100.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult r = selector.evaluateOnce();
    assertFalse(r.switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void ineligible_candidate_is_ignored() {
    SelectionPolicy policy = SelectionPolicy.builder()
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .build();

    FakeLink a = new FakeLink("A"); // current
    FakeLink b = new FakeLink("B"); // cheaper but FAILED
    a.setMetrics(new FakeMetrics().withLatencyMillis(120.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(10.0));
    b.setState(LinkState.FAILED);

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult r = selector.evaluateOnce();
    assertFalse(r.switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void single_link_repo_never_switches() {
    SelectionPolicy policy = SelectionPolicy.builder()
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .build();

    FakeLink a = new FakeLink("A");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult r = selector.evaluateOnce();
    assertFalse(r.switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void min_hold_time_blocks_switch_even_with_improvement() {
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.10)
        .minimumHoldTime(Duration.ofSeconds(30))
        .cooldownAfterSwitch(Duration.ZERO)
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(50.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    // First switch A->B to set lastSwitchTime
    assertTrue(selector.evaluateOnce().switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());

    // Immediately make A much better; hold time should block switch back
    a.setMetrics(new FakeMetrics().withLatencyMillis(10.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(20.0));
    SelectionResult r2 = selector.evaluateOnce();
    assertFalse(r2.switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void cooldown_is_bypassed_on_hard_failover() {
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.10)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ofSeconds(60))
        .hardMaxLatencyMillis(200.0)
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(80.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    // First switch A->B sets cooldown
    assertTrue(selector.evaluateOnce().switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());

    // Now make B exceed hard threshold; should switch to A despite cooldown
    b.setMetrics(new FakeMetrics().withLatencyMillis(10_000.0));
    a.setMetrics(new FakeMetrics().withLatencyMillis(50.0));
    SelectionResult r2 = selector.evaluateOnce();
    assertTrue(r2.switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void flap_limit_allows_switch_after_window_expires() throws Exception {
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.10)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .maxSwitchesPerWindow(2)
        .flapWindow(Duration.ofMillis(150))
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(80.0));
    assertTrue(selector.evaluateOnce().switched()); // A->B

    a.setMetrics(new FakeMetrics().withLatencyMillis(60.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(90.0));
    assertTrue(selector.evaluateOnce().switched()); // B->A

    a.setMetrics(new FakeMetrics().withLatencyMillis(95.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(60.0));
    assertFalse(selector.evaluateOnce().switched()); // blocked

    TimeUnit.MILLISECONDS.sleep(180); // window expiry

    assertTrue(selector.evaluateOnce().switched()); // allowed now
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void no_eligible_links_holds_current_without_exception() {
    SelectionPolicy policy = SelectionPolicy.builder().build();

    FakeLink current = new FakeLink("CURR");
    current.setMetrics(new FakeMetrics().withLatencyMillis(100.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(current);
    LinkRepository repo = List::of; // empty

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult r = selector.evaluateOnce();
    assertFalse(r.switched());
    assertEquals("CURR", switcher.getCurrentLink().getLinkId().value());
  }

  // ---------- Cost model coverage ----------

  @Test
  void stale_metrics_adds_penalty() {
    CostWeights w = CostWeights.builder().weightStaleMetricsPenalty(500.0).build();
    DefaultCostFunction cf = new DefaultCostFunction();

    FakeMetrics fresh = new FakeMetrics().withLatencyMillis(100.0).withWindow(Duration.ofMillis(50));
    FakeMetrics stale = new FakeMetrics().withLatencyMillis(100.0).withWindow(Duration.ofMillis(50));
    stale.withLastUpdated(Instant.now().minus(Duration.ofMillis(200)));

    double cFresh = cf.compute(fresh, w);
    double cStale = cf.compute(stale, w);
    assertTrue(cStale >= cFresh + 500.0 - 1e-9);
  }

  @Test
  void queue_penalty_is_capped() {
    CostWeights w = CostWeights.builder().weightOutboundQueuePenalty(1.0).maxQueueDepthForPenalty(100).build();
    DefaultCostFunction cf = new DefaultCostFunction();

    FakeMetrics q100 = new FakeMetrics().withOutboundQueueDepth(100);
    FakeMetrics qHuge = new FakeMetrics().withOutboundQueueDepth(1_000_000);

    double c100 = cf.compute(q100, w);
    double cHuge = cf.compute(qHuge, w);
    assertEquals(c100, cHuge, 1e-6);
  }

  @Test
  void missing_optionals_use_defaults_deterministically() {
    CostWeights w = CostWeights.builder().build();
    DefaultCostFunction cf = new DefaultCostFunction();

    FakeMetricsEmptyOptionals m = new FakeMetricsEmptyOptionals();
    double cost = cf.compute(m, w); // should not throw
    assertTrue(Double.isFinite(cost));
  }

  @Test
  void loss_and_error_are_clamped() {
    CostWeights w = CostWeights.builder().build();
    DefaultCostFunction cf = new DefaultCostFunction();

    FakeMetrics bad = new FakeMetrics().withLossRatio(2.0).withErrorRate(-5.0);
    FakeMetrics norm = new FakeMetrics().withLossRatio(1.0).withErrorRate(0.0);

    assertEquals(cf.compute(norm, w), cf.compute(bad, w), 1e-6);
  }

  // ---------- Orchestrator behavior ----------

  @Test
  void orchestrator_burst_across_multiple_links_is_coalesced_globally() throws Exception {
    EvaluateCountingSelector spy = new EvaluateCountingSelector();

    SelectionOrchestratorConfig cfg = SelectionOrchestratorConfig.builder()
        .minInterEventInterval(Duration.ofMillis(50))
        .enablePeriodicScan(false)
        .build();

    SelectionOrchestrator orch = SelectionOrchestrator.start(spy, cfg);

    LinkId a = new LinkId("A");
    LinkId b = new LinkId("B");
    FakeMetrics m = new FakeMetrics().withLatencyMillis(100.0);
    Instant t0 = Instant.now();

    orch.onLinkMetricsUpdated(new LinkMetricsUpdatedEvent(a, m, t0));
    orch.onLinkStateChanged(new LinkStateChangedEvent(a, LinkState.CONNECTED, LinkState.DEGRADED, t0.plusMillis(5)));
    orch.onLinkMetricsUpdated(new LinkMetricsUpdatedEvent(b, m, t0.plusMillis(10)));
    orch.onLinkStateChanged(new LinkStateChangedEvent(b, LinkState.CONNECTED, LinkState.CONNECTED, t0.plusMillis(15)));

    TimeUnit.MILLISECONDS.sleep(80);
    assertEquals(1, spy.getEvaluateCount());

    orch.close();
  }

  @Test
  void orchestrator_periodic_scan_triggers_evaluations() throws Exception {
    EvaluateCountingSelector spy = new EvaluateCountingSelector();

    SelectionOrchestratorConfig cfg = SelectionOrchestratorConfig.builder()
        .enablePeriodicScan(true)
        .periodicScanInterval(Duration.ofMillis(60))
        .build();

    SelectionOrchestrator orch = SelectionOrchestrator.start(spy, cfg);

    TimeUnit.MILLISECONDS.sleep(190);
    int n = spy.getEvaluateCount();
    assertTrue(n >= 2, "Expected >=2 periodic evaluations, got " + n);

    orch.close();
  }

  // ---------- Test helpers ----------

  private static final class TestLinkSwitcher implements LinkSwitcher {
    private final AtomicReference<Link> ref;
    private TestLinkSwitcher(Link initial) { this.ref = new AtomicReference<>(initial); }
    static TestLinkSwitcher withInitial(Link initial) { return new TestLinkSwitcher(initial); }
    @Override public Link getCurrentLink() { return ref.get(); }
    @Override public boolean switchTo(Link next, String reason) {
      if (next == null) return false;
      Link prev = ref.get();
      if (prev != null && prev.getLinkId().equals(next.getLinkId())) return false;
      ref.set(next);
      return true;
    }
  }

  private static final class FakeLink implements Link {
    private final LinkId id;
    private volatile LinkState state = LinkState.CONNECTED;
    private volatile LinkMetrics metrics;

    FakeLink(String id) { this.id = new LinkId(id); }
    void setMetrics(LinkMetrics m) { this.metrics = m; }
    void setState(LinkState s) { this.state = s; }

    @Override public LinkId getLinkId() { return id; }
    @Override public URI getRemoteUri() { return URI.create("mock://" + id.value()); }
    @Override public LinkState getState() { return state; }
    @Override public LinkMetrics getMetrics() { return metrics; }

    @Override
    public OptionalDouble getBaseCost() {
      return OptionalDouble.empty();
    }

    @Override public boolean isAvailable() { return state == LinkState.CONNECTED || state == LinkState.DEGRADED; }
    @Override public void connect() { state = LinkState.CONNECTED; }
    @Override public void disconnect() { state = LinkState.DISCONNECTED; }
  }

  private static class FakeMetrics implements LinkMetrics {
    private double latency = 100.0;
    private double jitter = 5.0;
    private double loss = 0.0;
    private double errors = 0.0;
    private int queue = 0;
    private Double price = 0.0;
    private Instant last = Instant.now();
    private Duration window = Duration.ofSeconds(5);

    FakeMetrics withLatencyMillis(double v) { latency = v; last = Instant.now(); return this; }
    FakeMetrics withJitterMillis(double v) { jitter = v; return this; }
    FakeMetrics withLossRatio(double v) { loss = v; return this; }
    FakeMetrics withErrorRate(double v) { errors = v; return this; }
    FakeMetrics withOutboundQueueDepth(int v) { queue = v; return this; }
    FakeMetrics withPricePerMiB(Double v) { price = v; return this; }
    FakeMetrics withWindow(Duration w) { window = w; return this; }
    FakeMetrics withLastUpdated(Instant t) { last = t; return this; }

    @Override public OptionalDouble getLatencyMillisEma() { return OptionalDouble.of(latency); }
    @Override public OptionalDouble getJitterMillisEma() { return OptionalDouble.of(jitter); }
    @Override public double getLossRatio() { return loss; }
    @Override public double getErrorRate() { return errors; }
    @Override public int getOutboundQueueDepth() { return queue; }
    @Override public OptionalDouble getThroughputMibPerSecond() { return OptionalDouble.empty(); }
    @Override public Instant getLastUpdated() { return last; }
    @Override public Duration getWindow() { return window; }
  }

  private static final class FakeMetricsEmptyOptionals extends FakeMetrics {
    @Override public OptionalDouble getLatencyMillisEma() { return OptionalDouble.empty(); }
    @Override public OptionalDouble getJitterMillisEma() { return OptionalDouble.empty(); }
  }

  private static final class EvaluateCountingSelector extends LinkSelector {
    private int count = 0;
    EvaluateCountingSelector() {
      super(new DefaultCostFunction(),
          CostWeights.builder().build(),
          SelectionPolicy.builder().build(),
          () -> List.of(),
          new LinkSwitcher() { @Override public Link getCurrentLink() { return null; } @Override public boolean switchTo(Link next, String reason) { return false; } });
    }
    @Override public SelectionResult evaluateOnce() {
      count++;
      return new SelectionResult(Instant.now(), new LinkId("curr"), Double.NaN, new LinkId("best"), Double.NaN, false, "noop");
    }
    int getEvaluateCount() { return count; }
  }
}
