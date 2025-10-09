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

/**
 * JUnit 5 tests for cost-based selection (hysteresis, hard-failover, cooldown, flap-limit)
 * and orchestrator debounce. Uses local fakes; no external logging.
 */
class LinkSelectorDecisionTest {

  @Test
  void switches_when_improvement_exceeds_hysteresis_and_held_long_enough() {
    CostWeights weights = CostWeights.builder().build();
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.15)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(80.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(weights)
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult result = selector.evaluateOnce();
    assertTrue(result.switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void does_not_switch_within_hysteresis_band() {
    CostWeights weights = CostWeights.builder().build();
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.25)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(78.0)); // 22% better < 25%

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(weights)
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult result = selector.evaluateOnce();
    assertFalse(result.switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void hard_failover_ignores_hysteresis_and_cooldown() {
    CostWeights weights = CostWeights.builder().build();
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.90)
        .minimumHoldTime(Duration.ofHours(1))
        .cooldownAfterSwitch(Duration.ofHours(1))
        .hardMaxLatencyMillis(200.0)
        .build();

    FakeLink bad = new FakeLink("A");
    FakeLink good = new FakeLink("B");
    bad.setMetrics(new FakeMetrics().withLatencyMillis(10_000.0));
    good.setMetrics(new FakeMetrics().withLatencyMillis(50.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(bad);
    LinkRepository repo = () -> List.of(bad, good);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(weights)
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult result = selector.evaluateOnce();
    assertTrue(result.switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void cooldown_blocks_back_to_back_hysteresis_switches() {
    CostWeights weights = CostWeights.builder().build();
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.10)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ofSeconds(5))
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(80.0));

    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(weights)
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    SelectionResult first = selector.evaluateOnce();
    assertTrue(first.switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());

    // Immediately reverse advantage; still within cooldown → no switch back
    a.setMetrics(new FakeMetrics().withLatencyMillis(70.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(75.0));

    SelectionResult second = selector.evaluateOnce();
    assertFalse(second.switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void flap_limit_blocks_excessive_switching() {
    CostWeights weights = CostWeights.builder().build();
    SelectionPolicy policy = SelectionPolicy.builder()
        .hysteresisRatio(0.10)
        .minimumHoldTime(Duration.ZERO)
        .cooldownAfterSwitch(Duration.ZERO)
        .maxSwitchesPerWindow(2)
        .flapWindow(Duration.ofSeconds(60))
        .build();

    FakeLink a = new FakeLink("A");
    FakeLink b = new FakeLink("B");
    TestLinkSwitcher switcher = TestLinkSwitcher.withInitial(a);
    LinkRepository repo = () -> List.of(a, b);

    LinkSelector selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(weights)
        .selectionPolicy(policy)
        .linkRepository(repo)
        .linkSwitcher(switcher)
        .build();

    // 1: A->B
    a.setMetrics(new FakeMetrics().withLatencyMillis(100.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(80.0));
    assertTrue(selector.evaluateOnce().switched());
    assertEquals("B", switcher.getCurrentLink().getLinkId().value());

    // 2: B->A
    a.setMetrics(new FakeMetrics().withLatencyMillis(70.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(90.0));
    assertTrue(selector.evaluateOnce().switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());

    // 3: attempt within flap window → blocked
    a.setMetrics(new FakeMetrics().withLatencyMillis(95.0));
    b.setMetrics(new FakeMetrics().withLatencyMillis(60.0));
    SelectionResult third = selector.evaluateOnce();
    assertFalse(third.switched());
    assertEquals("A", switcher.getCurrentLink().getLinkId().value());
  }

  @Test
  void orchestrator_debounces_bursty_events() throws Exception {
    EvaluateCountingSelector spy = new EvaluateCountingSelector();

    SelectionOrchestratorConfig cfg = SelectionOrchestratorConfig.builder()
        .minInterEventInterval(Duration.ofMillis(50))
        .enablePeriodicScan(false)
        .build();

    SelectionOrchestrator orch = SelectionOrchestrator.start(spy, cfg);

    LinkId id = new LinkId("X");
    FakeMetrics metrics = new FakeMetrics().withLatencyMillis(100.0);
    Instant base = Instant.now();

    orch.onLinkMetricsUpdated(new LinkMetricsUpdatedEvent(id, metrics, base));
    orch.onLinkMetricsUpdated(new LinkMetricsUpdatedEvent(id, metrics, base.plusMillis(5)));
    orch.onLinkStateChanged(new LinkStateChangedEvent(id, LinkState.CONNECTED, LinkState.CONNECTED, base.plusMillis(10)));

    TimeUnit.MILLISECONDS.sleep(80);
    assertEquals(1, spy.getEvaluateCount());

    orch.onLinkMetricsUpdated(new LinkMetricsUpdatedEvent(id, metrics, base.plusMillis(140)));
    orch.onLinkStateChanged(new LinkStateChangedEvent(id, LinkState.CONNECTED, LinkState.DEGRADED, base.plusMillis(150)));

    TimeUnit.MILLISECONDS.sleep(80);
    assertEquals(2, spy.getEvaluateCount());

    orch.close();
  }

  // ---- Test fakes / helpers ----

  private static final class TestLinkSwitcher implements LinkSwitcher {
    private final AtomicReference<Link> ref;

    private TestLinkSwitcher(Link initial) {
      this.ref = new AtomicReference<>(initial);
    }

    static TestLinkSwitcher withInitial(Link initial) {
      return new TestLinkSwitcher(initial);
    }

    @Override
    public Link getCurrentLink() {
      return ref.get();
    }

    @Override
    public boolean switchTo(Link next, String reason) {
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

    FakeLink(String id) {
      this.id = new LinkId(id);
    }

    @Override
    public LinkId getLinkId() {
      return id;
    }

    @Override
    public URI getRemoteUri() {
      return URI.create("mock://" + id.value());
    }

    @Override
    public LinkState getState() {
      return state;
    }

    void setState(LinkState s) {
      this.state = s;
    }

    @Override
    public LinkMetrics getMetrics() {
      return metrics;
    }

    void setMetrics(LinkMetrics m) {
      this.metrics = m;
    }

    @Override
    public boolean isAvailable() {
      return state == LinkState.CONNECTED || state == LinkState.DEGRADED;
    }

    @Override
    public void connect() {
      state = LinkState.CONNECTED;
    }

    @Override
    public void disconnect() {
      state = LinkState.DISCONNECTED;
    }
  }

  private static final class FakeMetrics implements LinkMetrics {
    private double latency = 100.0;
    private double jitter = 5.0;
    private double loss = 0.0;
    private double errors = 0.0;
    private int queue = 0;
    private Double price = 0.0;
    private Instant last = Instant.now();
    private Duration window = Duration.ofSeconds(5);

    FakeMetrics withLatencyMillis(double v) {
      latency = v;
      last = Instant.now();
      return this;
    }

    FakeMetrics withJitterMillis(double v) {
      jitter = v;
      return this;
    }

    FakeMetrics withLossRatio(double v) {
      loss = v;
      return this;
    }

    FakeMetrics withErrorRate(double v) {
      errors = v;
      return this;
    }

    FakeMetrics withOutboundQueueDepth(int v) {
      queue = v;
      return this;
    }

    FakeMetrics withPricePerMiB(Double v) {
      price = v;
      return this;
    }

    FakeMetrics withWindow(Duration w) {
      window = w;
      return this;
    }

    FakeMetrics withLastUpdated(Instant t) {
      last = t;
      return this;
    }

    @Override
    public OptionalDouble getLatencyMillisEma() {
      return OptionalDouble.of(latency);
    }

    @Override
    public OptionalDouble getJitterMillisEma() {
      return OptionalDouble.of(jitter);
    }

    @Override
    public double getLossRatio() {
      return loss;
    }

    @Override
    public double getErrorRate() {
      return errors;
    }

    @Override
    public int getOutboundQueueDepth() {
      return queue;
    }

    @Override
    public OptionalDouble getThroughputMibPerSecond() {
      return OptionalDouble.empty();
    }

    @Override
    public Instant getLastUpdated() {
      return last;
    }

    @Override
    public Duration getWindow() {
      return window;
    }
  }

  /**
   * Spy that counts evaluateOnce() invocations; usable by the orchestrator test.
   */
  /** Spy that counts evaluateOnce() invocations; usable by the orchestrator test. */
  private static final class EvaluateCountingSelector extends LinkSelector {
    private int count = 0;

    EvaluateCountingSelector() {
      super(
          new DefaultCostFunction(),
          CostWeights.builder().build(),
          SelectionPolicy.builder().build(),
          () -> List.of(), // empty repo; not used by the spy
          new LinkSwitcher() {
            @Override public Link getCurrentLink() { return null; }
            @Override public boolean switchTo(Link next, String reason) { return false; }
          }
      );
    }

    @Override
    public SelectionResult evaluateOnce() {
      count++;
      return new SelectionResult(
          Instant.now(),
          new LinkId("curr"),
          Double.NaN,
          new LinkId("best"),
          Double.NaN,
          false,
          "noop"
      );
    }

    int getEvaluateCount() { return count; }
  }

}
