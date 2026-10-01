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

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Timeline of controlled failure injections (IC26 KPI clarification Q10, Q12-Q14). Each run is
 * timed from the injection timestamp supplied by the test tooling:
 *
 * <ul>
 *   <li><b>detection</b> - first machine-generated flag after the injection: a feed outage, an MTI
 *       cache rebuild, or a critical KPI's raw band worse than at injection (MTTD; operator
 *       reaction is recorded separately as operator awareness, never mixed in);</li>
 *   <li><b>degraded</b> - overall displayed status worse than at injection;</li>
 *   <li><b>mitigation</b> / <b>operator aware</b> - reported by the tooling or operator;</li>
 *   <li><b>usable restored</b> - no critical KPI red any more, after having been red;</li>
 *   <li><b>fully restored</b> - all critical KPIs green again; closes the run.</li>
 * </ul>
 *
 * Restoration is dated to when the KPIs actually recovered (see {@link BandTracker}). Durations
 * are kept per failure type plus an aggregate ({@link #ALL}).
 */
final class FaultRunTracker {

  static final String ALL = "ALL";

  enum Stage {
    DETECTION,
    DEGRADED,
    MITIGATION,
    OPERATOR_AWARE,
    USABLE_RESTORED,
    FULLY_RESTORED
  }

  /** Persisted as JSON so a run survives a MAPS restart (the RESTART failure type). */
  static final class FaultRun {
    String runId;
    String failureType;
    long injectedAt;
    Map<String, Integer> baselineBands = new TreeMap<>();
    int baselineOverall;
    Long detectedAt;
    String detectedBy;
    Long degradedAt;
    boolean wentRed;
    Long mitigationAt;
    Long operatorAwareAt;
    Long usableRestoredAt;
    Long fullyRestoredAt;
    String outcome;
    Long closedAt;
  }

  static final class RunTypeStats {
    private final Map<Stage, DurationStats> stages = new EnumMap<>(Stage.class);
    private long runs;
    private long notRestored;
    private long noEffect;

    private RunTypeStats() {
      for (Stage stage : Stage.values()) {
        stages.put(stage, new DurationStats());
      }
    }

    DurationStats stage(Stage stage) {
      return stages.get(stage);
    }

    synchronized long getRuns() {
      return runs;
    }

    synchronized long getNotRestored() {
      return notRestored;
    }

    synchronized long getNoEffect() {
      return noEffect;
    }
  }

  private final Duration runTimeout;
  private final Map<String, FaultRun> open = new LinkedHashMap<>();
  private final Map<String, RunTypeStats> stats = new TreeMap<>();

  FaultRunTracker(Duration runTimeout) {
    this.runTimeout = runTimeout;
    stats.put(ALL, new RunTypeStats());
  }

  synchronized FaultRun inject(String runId, String failureType, Instant at, Map<KpiId, Band> rawBands, Band overall) {
    FaultRun run = new FaultRun();
    run.runId = runId;
    run.failureType = failureType;
    run.injectedAt = at.toEpochMilli();
    rawBands.forEach((kpi, band) -> run.baselineBands.put(kpi.name(), band.getCode()));
    run.baselineOverall = overall.getCode();
    open.put(runId, run);
    stats.computeIfAbsent(failureType, type -> new RunTypeStats());
    return run;
  }

  /** A machine-generated problem flag, e.g. {@code feed:mti:status} or {@code cache_rebuild}. */
  synchronized void onMachineFlag(String flag, Instant at) {
    for (FaultRun run : open.values()) {
      if (run.detectedAt == null && at.toEpochMilli() >= run.injectedAt) {
        run.detectedAt = at.toEpochMilli();
        run.detectedBy = flag;
      }
    }
  }

  /**
   * @param usableSince when "no critical KPI red" last became true, {@code null} while not usable
   * @param fullySince when "all critical KPIs green" last became true, {@code null} while not
   * @return runs closed by this evaluation
   */
  synchronized List<FaultRun> onEvaluation(
      Map<KpiId, Band> rawBands,
      Band overall,
      Instant overallSince,
      Instant usableSince,
      Instant fullySince,
      Instant now) {
    List<FaultRun> closed = new ArrayList<>();
    Iterator<FaultRun> iterator = open.values().iterator();
    while (iterator.hasNext()) {
      FaultRun run = iterator.next();
      if (now.toEpochMilli() < run.injectedAt) {
        continue;
      }
      if (run.detectedAt == null) {
        detectFromKpis(run, rawBands, now);
      }
      if (overall != Band.NO_DATA && overall.getCode() > run.baselineOverall) {
        if (run.degradedAt == null) {
          run.degradedAt = Math.max(run.injectedAt, (overallSince == null ? now : overallSince).toEpochMilli());
        }
        if (overall == Band.RED) {
          run.wentRed = true;
        }
      }
      if (run.degradedAt != null) {
        if (usableSince == null) {
          run.usableRestoredAt = null;
        } else if (run.wentRed && run.usableRestoredAt == null) {
          run.usableRestoredAt = Math.max(run.degradedAt, usableSince.toEpochMilli());
        }
        if (fullySince != null) {
          run.fullyRestoredAt = Math.max(run.degradedAt, fullySince.toEpochMilli());
          close(run, "RESTORED", now);
          iterator.remove();
          closed.add(run);
          continue;
        }
      }
      if (now.toEpochMilli() - run.injectedAt > runTimeout.toMillis()) {
        close(run, run.degradedAt == null && run.detectedAt == null ? "NO_EFFECT" : "NOT_RESTORED", now);
        iterator.remove();
        closed.add(run);
      }
    }
    return closed;
  }

  private void detectFromKpis(FaultRun run, Map<KpiId, Band> rawBands, Instant now) {
    for (Map.Entry<KpiId, Band> entry : rawBands.entrySet()) {
      Integer baseline = run.baselineBands.get(entry.getKey().name());
      if (entry.getKey().isCritical() && entry.getValue() != Band.NO_DATA
          && baseline != null && entry.getValue().getCode() > baseline) {
        run.detectedAt = now.toEpochMilli();
        run.detectedBy = "kpi:" + entry.getKey().name();
        return;
      }
    }
  }

  synchronized boolean mark(String runId, Stage stage, Instant at) {
    FaultRun run = runId == null ? latestOpen() : open.get(runId);
    if (run == null) {
      return false;
    }
    if (stage == Stage.MITIGATION && run.mitigationAt == null) {
      run.mitigationAt = at.toEpochMilli();
    } else if (stage == Stage.OPERATOR_AWARE && run.operatorAwareAt == null) {
      run.operatorAwareAt = at.toEpochMilli();
    }
    return true;
  }

  /** Ends a run before it fully restored, e.g. when the test window closes. */
  synchronized FaultRun end(String runId, Instant now) {
    FaultRun run = runId == null ? latestOpen() : open.get(runId);
    if (run == null) {
      return null;
    }
    open.remove(run.runId);
    close(run, run.degradedAt == null && run.detectedAt == null ? "NO_EFFECT" : "ENDED", now);
    return run;
  }

  private FaultRun latestOpen() {
    FaultRun latest = null;
    for (FaultRun run : open.values()) {
      latest = run;
    }
    return latest;
  }

  private void close(FaultRun run, String outcome, Instant now) {
    run.outcome = outcome;
    run.closedAt = now.toEpochMilli();
    for (RunTypeStats typeStats : List.of(stats.get(ALL), stats.computeIfAbsent(run.failureType, type -> new RunTypeStats()))) {
      synchronized (typeStats) {
        typeStats.runs++;
        if ("NOT_RESTORED".equals(outcome)) {
          typeStats.notRestored++;
        }
        if ("NO_EFFECT".equals(outcome)) {
          typeStats.noEffect++;
        }
      }
      if ("NO_EFFECT".equals(outcome)) {
        continue;
      }
      record(typeStats, Stage.DETECTION, run, run.detectedAt, true);
      record(typeStats, Stage.DEGRADED, run, run.degradedAt, false);
      record(typeStats, Stage.MITIGATION, run, run.mitigationAt, false);
      record(typeStats, Stage.OPERATOR_AWARE, run, run.operatorAwareAt, false);
      record(typeStats, Stage.USABLE_RESTORED, run, run.usableRestoredAt, run.wentRed);
      record(typeStats, Stage.FULLY_RESTORED, run, run.fullyRestoredAt, run.degradedAt != null);
    }
  }

  private static void record(RunTypeStats typeStats, Stage stage, FaultRun run, Long at, boolean expected) {
    if (at != null) {
      typeStats.stage(stage).record(Duration.ofMillis(at - run.injectedAt));
    } else if (expected) {
      typeStats.stage(stage).recordNotReached();
    }
  }

  synchronized Map<String, RunTypeStats> getStats() {
    return new TreeMap<>(stats);
  }

  synchronized RunTypeStats statsFor(String failureType) {
    return stats.computeIfAbsent(failureType, type -> new RunTypeStats());
  }

  synchronized int getOpenCount() {
    return open.size();
  }

  synchronized List<FaultRun> exportOpen() {
    return new ArrayList<>(open.values());
  }

  synchronized void restore(Collection<FaultRun> runs) {
    if (runs == null) {
      return;
    }
    for (FaultRun run : runs) {
      open.put(run.runId, run);
      stats.computeIfAbsent(run.failureType, type -> new RunTypeStats());
    }
  }

  /** Flat view for the event log and the post-exercise export, durations in seconds. */
  static Map<String, Object> summary(FaultRun run) {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("run_id", run.runId);
    summary.put("failure_type", run.failureType);
    summary.put("outcome", run.outcome);
    summary.put("injected_at", Instant.ofEpochMilli(run.injectedAt).toString());
    putStage(summary, "detected", run, run.detectedAt);
    summary.put("detected_by", run.detectedBy);
    putStage(summary, "degraded", run, run.degradedAt);
    summary.put("went_red", run.wentRed);
    putStage(summary, "mitigation", run, run.mitigationAt);
    putStage(summary, "operator_aware", run, run.operatorAwareAt);
    putStage(summary, "usable_restored", run, run.usableRestoredAt);
    putStage(summary, "fully_restored", run, run.fullyRestoredAt);
    return summary;
  }

  private static void putStage(Map<String, Object> summary, String name, FaultRun run, Long at) {
    summary.put(name + "_at", at == null ? null : Instant.ofEpochMilli(at).toString());
    summary.put(name + "_seconds", at == null ? null : (at - run.injectedAt) / 1000.0);
  }
}
