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

package io.mapsmessaging.state.kpi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.tak.ClassificationRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusRegistry;
import io.mapsmessaging.state.drone.tak.MtiStatusSnapshot;
import io.mapsmessaging.state.metrics.FeedActivityRegistry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Evaluates the IC26 mission KPIs once per tick on the mission roster, bands them, derives the
 * overall picture status, and feeds fault run, MTI cache rebuild and feed outage timing. All
 * transitions go to the {@link KpiEventLog}; the JMX beans read the latest state from here.
 */
final class KpiEvaluator implements MtiStatusRegistry.StatusListener {

  /** What the evaluator needs to know about a twin. */
  record TwinInfo(String uid, TwinType twinType, Instant lastSeenAt) {
  }

  /** Inputs, injectable for tests. */
  record Sources(
      Supplier<Collection<TwinInfo>> twins,
      Function<String, MtiStatusSnapshot> mti,
      Function<String, ClassificationRegistry.Classification> classifications,
      Supplier<Map<String, FeedActivityRegistry.FeedState>> feeds,
      BooleanSupplier clearMtiCache) {
  }

  private static final Duration SAVE_INTERVAL = Duration.ofSeconds(5);

  private final KpiConfig config;
  private final Sources sources;
  private final Clock clock;
  private final KpiEventLog eventLog;
  private final KpiStateStore stateStore;
  private final MissionRoster roster;
  private final Map<KpiId, BandTracker> trackers = new EnumMap<>(KpiId.class);
  private final FaultRunTracker runs;
  private final CacheRebuildTracker rebuilds;
  private final FeedOutageMonitor feedMonitor;

  private KpiSnapshot latest;
  private Band overall = Band.NO_DATA;
  private Instant overallSince;
  private Instant usableSince;
  private Instant fullySince;
  private Set<String> lastFreshMti = Set.of();
  private Instant lastSnapshotLog;
  private Instant lastSave;

  KpiEvaluator(KpiConfig config, Sources sources, Clock clock, KpiEventLog eventLog, KpiStateStore stateStore) {
    this.config = config;
    this.sources = sources;
    this.clock = clock;
    this.eventLog = eventLog;
    this.stateStore = stateStore;
    this.roster = new MissionRoster(config);
    for (KpiId kpi : KpiId.values()) {
      if (config.band(kpi) != null) {
        trackers.put(kpi, new BandTracker(config.getRecoveryHold()));
      }
    }
    this.runs = new FaultRunTracker(config.getRunTimeout());
    this.rebuilds = new CacheRebuildTracker(config.getRebuildTargetFraction(), config.getRebuildTimeout());
    this.feedMonitor = new FeedOutageMonitor(config, clock.instant());
  }

  /** Restores the persisted roster and open runs; a saved MTI baseline means the cache was lost. */
  synchronized void start() {
    Instant now = clock.instant();
    KpiStateStore.State state = stateStore.load();
    Map<String, Object> fields = new LinkedHashMap<>();
    if (state != null) {
      roster.restore(state.roster, state.mtiAnnounced);
      runs.restore(state.openRuns);
      fields.put("restored_roster", state.roster == null ? 0 : state.roster.size());
      fields.put("restored_open_runs", state.openRuns == null ? 0 : state.openRuns.size());
      if (rebuilds.start(CacheRebuildTracker.Trigger.RESTART, now, state.freshMti)) {
        runs.onMachineFlag("cache_rebuild:RESTART", now);
        eventLog.log(now, "cache_rebuild_start", Map.of("trigger", "RESTART", "baseline", state.freshMti.size()));
      }
    }
    eventLog.log(now, "kpi_start", fields);
    lastSave = now;
  }

  synchronized void stop() {
    save(clock.instant());
    eventLog.log(clock.instant(), "kpi_stop", Map.of());
  }

  synchronized void evaluate() {
    Instant now = clock.instant();
    Map<String, TwinInfo> twins = new HashMap<>();
    for (TwinInfo twin : sources.twins().get()) {
      if (config.getTwinTypes().contains(twin.twinType())) {
        twins.put(twin.uid(), twin);
        roster.observe(twin.uid(), twin.lastSeenAt() == null ? now : twin.lastSeenAt());
      }
    }
    for (String removed : roster.expire(now)) {
      ClassificationRegistry.forget(removed);
      eventLog.log(now, "roster_removed", Map.of("uid", removed, "reason", "admin_timeout"));
    }

    List<AssetAssessment> assets = new ArrayList<>();
    for (String uid : roster.members()) {
      TwinInfo twin = twins.get(uid);
      assets.add(KpiCalculator.assess(
          uid,
          roster.isEligible(uid),
          twin == null ? null : twin.lastSeenAt(),
          sources.mti().apply(uid),
          roster.wasAnnounced(uid),
          sources.classifications().apply(uid),
          config.getExpectedClassifications().get(uid),
          config,
          now));
    }
    latest = KpiCalculator.compute(assets);
    lastFreshMti = latest.freshMtiUids();

    Map<KpiId, Band> rawBands = updateBands(now);
    updateOverall(now);

    for (FeedOutageMonitor.OutageEvent outage : feedMonitor.update(sources.feeds().get(), now)) {
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("feed", outage.feed());
      fields.put("at", outage.at().toString());
      fields.put("duration_seconds", outage.duration() == null ? null : outage.duration().toMillis() / 1000.0);
      eventLog.log(outage.detectedAt(), outage.started() ? "feed_outage_start" : "feed_outage_end", fields);
      if (outage.started()) {
        runs.onMachineFlag("feed:" + outage.feed(), outage.detectedAt());
      }
    }

    for (FaultRunTracker.FaultRun closed : runs.onEvaluation(rawBands, overall, overallSince, usableSince, fullySince, now)) {
      eventLog.log(now, "run_summary", FaultRunTracker.summary(closed));
    }
    for (CacheRebuildTracker.RebuildEvent rebuild : rebuilds.update(lastFreshMti, now)) {
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("trigger", rebuild.trigger().name());
      fields.put("target", rebuild.target().name());
      fields.put("reached", rebuild.reached());
      fields.put("seconds", rebuild.duration().toMillis() / 1000.0);
      fields.put("baseline", rebuild.baselineSize());
      eventLog.log(now, "cache_rebuild", fields);
    }

    if (lastSnapshotLog == null || !now.isBefore(lastSnapshotLog.plus(config.getSnapshotInterval()))) {
      lastSnapshotLog = now;
      eventLog.log(now, "kpi_snapshot", snapshotFields());
    }
    if (lastSave == null || !now.isBefore(lastSave.plus(SAVE_INTERVAL))) {
      save(now);
    }
  }

  private Map<KpiId, Band> updateBands(Instant now) {
    Map<KpiId, Band> rawBands = new EnumMap<>(KpiId.class);
    for (Map.Entry<KpiId, BandTracker> entry : trackers.entrySet()) {
      KpiId kpi = entry.getKey();
      Band raw = config.band(kpi).evaluate(latest.value(kpi).value());
      rawBands.put(kpi, raw);
      BandTracker.Transition transition = entry.getValue().update(raw, now);
      if (transition != null) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kpi", kpi.name());
        fields.put("critical", kpi.isCritical());
        fields.put("from", transition.from().name());
        fields.put("to", transition.to().name());
        fields.put("effective_at", transition.effectiveAt().toString());
        fields.put("value", finite(latest.value(kpi).value()));
        eventLog.log(now, "band_change", fields);
      }
    }
    return rawBands;
  }

  /**
   * Overall = worst displayed critical band (clarification Q8). With assets on the roster, a
   * critical KPI without data counts as amber: absence of evidence is not evidence of health.
   * Usable = no critical KPI red; fully restored = all green (Q10). Their timestamps come from the
   * band trackers, so they mark when the KPIs recovered, not when the hold time ran out.
   */
  private void updateOverall(Instant now) {
    Band worst = Band.GREEN;
    Instant usableAt = null;
    Instant greenAt = null;
    boolean anyRed = false;
    boolean allGreen = true;
    for (Map.Entry<KpiId, BandTracker> entry : trackers.entrySet()) {
      if (!entry.getKey().isCritical()) {
        continue;
      }
      BandTracker tracker = entry.getValue();
      Band displayed = tracker.getDisplayed();
      worst = Band.worst(worst, displayed == Band.NO_DATA ? Band.AMBER : displayed);
      anyRed |= displayed == Band.RED;
      allGreen &= displayed == Band.GREEN;
      usableAt = latest(usableAt, tracker.getNotRedSince());
      greenAt = latest(greenAt, tracker.getGreenSince());
    }
    if (latest.rosterSize() == 0) {
      worst = Band.NO_DATA;
      anyRed = true;
      allGreen = false;
    }
    usableSince = anyRed ? null : (usableAt == null ? now : usableAt);
    fullySince = allGreen ? (greenAt == null ? now : greenAt) : null;
    if (worst != overall) {
      Band previous = overall;
      overall = worst;
      if (worst.isWorseThan(previous) || previous == Band.NO_DATA || worst == Band.NO_DATA) {
        overallSince = now;
      } else {
        overallSince = worst == Band.GREEN ? fullySince : usableSince;
      }
      if (overallSince == null) {
        overallSince = now;
      }
      Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("from", previous.name());
      fields.put("to", worst.name());
      fields.put("effective_at", overallSince.toString());
      fields.put("usable", usableSince != null);
      fields.put("fully_restored", fullySince != null);
      eventLog.log(now, "overall_change", fields);
    }
  }

  private static Instant latest(Instant current, Instant candidate) {
    if (candidate == null) {
      return current;
    }
    return current == null || candidate.isAfter(current) ? candidate : current;
  }

  //<editor-fold desc="Control messages">
  /**
   * Handles a JSON control message from the test tooling or an operator, e.g.
   * {@code {"event":"inject","run_id":"feed-loss-1","failure_type":"FEED_LOSS"}}. Events: inject,
   * mitigation, operator_aware, end, deactivate, end_mission, note.
   */
  synchronized void handleControl(String json) {
    Instant now = clock.instant();
    JsonObject message;
    try {
      JsonElement parsed = JsonParser.parseString(json);
      message = parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
    } catch (RuntimeException e) {
      message = null;
    }
    String event = message == null ? null : text(message, "event");
    if (event == null) {
      eventLog.log(now, "control_rejected", Map.of("reason", "not a JSON object with an event"));
      return;
    }
    Instant at = timestamp(message, now);
    String runId = text(message, "run_id");
    switch (event.toLowerCase(Locale.ROOT)) {
      case "inject" -> inject(message, runId, at, now);
      case "mitigation" -> mark(runId, FaultRunTracker.Stage.MITIGATION, at, "mitigation");
      case "operator_aware" -> mark(runId, FaultRunTracker.Stage.OPERATOR_AWARE, at, "operator_aware");
      case "end" -> {
        FaultRunTracker.FaultRun run = runs.end(runId, now);
        if (run != null) {
          eventLog.log(now, "run_summary", FaultRunTracker.summary(run));
        }
      }
      case "deactivate" -> {
        String uid = text(message, "uid");
        if (uid != null && roster.deactivate(uid)) {
          ClassificationRegistry.forget(uid);
          eventLog.log(now, "roster_removed", Map.of("uid", uid, "reason", "deactivated"));
        }
      }
      case "end_mission" -> {
        List<String> removed = roster.endMission();
        removed.forEach(ClassificationRegistry::forget);
        eventLog.log(now, "end_mission", Map.of("removed", removed.size()));
      }
      case "note" -> eventLog.log(at, "note", Map.of("text", String.valueOf(text(message, "text"))));
      default -> eventLog.log(now, "control_rejected", Map.of("reason", "unknown event " + event));
    }
    save(now);
  }

  private void inject(JsonObject message, String runId, Instant at, Instant now) {
    String failureType = text(message, "failure_type");
    if (failureType == null) {
      eventLog.log(now, "control_rejected", Map.of("reason", "inject without failure_type"));
      return;
    }
    failureType = failureType.toUpperCase(Locale.ROOT);
    String id = runId == null ? failureType + "-" + at.toEpochMilli() : runId;
    Map<KpiId, Band> rawBands = new EnumMap<>(KpiId.class);
    trackers.forEach((kpi, tracker) -> rawBands.put(kpi, tracker.getRaw()));
    runs.inject(id, failureType, at, rawBands, overall);
    eventLog.log(at, "inject", Map.of("run_id", id, "failure_type", failureType, "baseline_overall", overall.name()));

    boolean clearCache = message.has("clear_mti_cache") && message.get("clear_mti_cache").getAsBoolean();
    if (clearCache) {
      if (!config.isFaultInjection()) {
        eventLog.log(now, "control_rejected", Map.of("reason", "clear_mti_cache needs faultInjection: true"));
      } else if (sources.clearMtiCache().getAsBoolean()) {
        eventLog.log(now, "mti_cache_cleared", Map.of("run_id", id));
        if (rebuilds.start(CacheRebuildTracker.Trigger.CACHE_LOSS, now, lastFreshMti)) {
          runs.onMachineFlag("cache_rebuild:CACHE_LOSS", now);
          eventLog.log(now, "cache_rebuild_start", Map.of("trigger", "CACHE_LOSS", "baseline", lastFreshMti.size()));
        }
      }
    }
  }

  private void mark(String runId, FaultRunTracker.Stage stage, Instant at, String type) {
    if (runs.mark(runId, stage, at)) {
      eventLog.log(at, type, Map.of("run_id", String.valueOf(runId)));
    } else {
      eventLog.log(at, "control_rejected", Map.of("reason", type + " without an open run"));
    }
  }

  private static String text(JsonObject message, String key) {
    return message.has(key) && message.get(key).isJsonPrimitive() ? message.get(key).getAsString() : null;
  }

  private static Instant timestamp(JsonObject message, Instant now) {
    String at = text(message, "at");
    if (at == null) {
      return now;
    }
    try {
      return Instant.parse(at);
    } catch (DateTimeParseException e) {
      return now;
    }
  }
  //</editor-fold>

  //<editor-fold desc="MTI status listener">
  @Override
  public void onStatusAccepted(String twinId, Instant receivedAt) {
    roster.mtiAnnounced(twinId);
  }

  @Override
  public void onStatusCleared(String twinId) {
    roster.mtiWithdrawn(twinId);
  }
  //</editor-fold>

  private void save(Instant now) {
    lastSave = now;
    KpiStateStore.State state = new KpiStateStore.State();
    state.savedAt = now.toString();
    state.roster.putAll(roster.exportLastSeen());
    state.mtiAnnounced.addAll(roster.exportAnnounced());
    state.freshMti.addAll(lastFreshMti);
    state.openRuns.addAll(runs.exportOpen());
    stateStore.save(state);
  }

  private Map<String, Object> snapshotFields() {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("overall", overall.name());
    fields.put("usable", usableSince != null);
    fields.put("fully_restored", fullySince != null);
    fields.put("roster", latest.rosterSize());
    fields.put("eligible", latest.eligibleSize());
    Map<String, Object> kpis = new LinkedHashMap<>();
    for (KpiId kpi : KpiId.values()) {
      KpiSnapshot.Value value = latest.value(kpi);
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("value", finite(value.value()));
      entry.put("numerator", value.numerator());
      entry.put("denominator", value.denominator());
      BandTracker tracker = trackers.get(kpi);
      if (tracker != null) {
        entry.put("raw_band", tracker.getRaw().name());
        entry.put("band", tracker.getDisplayed().name());
      }
      kpis.put(kpi.name(), entry);
    }
    fields.put("kpis", kpis);
    Map<String, Integer> reasons = new LinkedHashMap<>();
    latest.unknownReasons().forEach((reason, count) -> reasons.put(reason.name(), count));
    fields.put("unknown_reasons", reasons);
    return fields;
  }

  private static Double finite(double value) {
    return Double.isNaN(value) ? null : value;
  }

  //<editor-fold desc="Read by the JMX beans">
  synchronized KpiSnapshot getLatest() {
    return latest;
  }

  synchronized Band getRawBand(KpiId kpi) {
    BandTracker tracker = trackers.get(kpi);
    return tracker == null ? Band.NO_DATA : tracker.getRaw();
  }

  synchronized Band getDisplayedBand(KpiId kpi) {
    BandTracker tracker = trackers.get(kpi);
    return tracker == null ? Band.NO_DATA : tracker.getDisplayed();
  }

  synchronized Band getOverall() {
    return overall;
  }

  synchronized boolean isUsable() {
    return usableSince != null;
  }

  synchronized boolean isFullyRestored() {
    return fullySince != null;
  }

  KpiConfig getConfig() {
    return config;
  }

  FaultRunTracker getRuns() {
    return runs;
  }

  CacheRebuildTracker getRebuilds() {
    return rebuilds;
  }

  FeedOutageMonitor getFeedMonitor() {
    return feedMonitor;
  }

  Clock getClock() {
    return clock;
  }
  //</editor-fold>
}
