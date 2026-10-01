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

import io.mapsmessaging.MapsEnvironment;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.drone.core.TwinType;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * KPI evaluator settings from {@code stateAdapters.kpi} in {@code TwinManager.yaml}. Every
 * threshold is configurable so the values can be frozen before SHINE (IC26 KPI clarification Q5);
 * the defaults are the provisional bands agreed for the exercise (Q6).
 *
 * <p>Uid lists are comma-separated strings: the configuration library does not keep YAML lists
 * of plain values.
 *
 * <pre>
 * stateAdapters:
 *   kpi:
 *     positionMaxAgeSeconds: 30        # position/telemetry freshness
 *     mtiMaxAgeSeconds: 120            # MTI status freshness (on top of MTI's own valid_until)
 *     recoveryHoldSeconds: 15          # a band only improves after holding this long (Q9)
 *     rosterAdminTimeoutSeconds: 3600  # silent assets leave the roster after this; 0 = never
 *     eligibilityMode: all             # all | list | announced | list_and_announced (Q2)
 *     eligibleUids: "UAS-1,UAS-2"
 *     rosterUids: ""                   # assets expected in the mission even before they report
 *     expectedClassifications:         # scripted assets, '*' matches any segment (Q19)
 *       "UAS-1": "a-*-A-M-F-Q"
 *     bands:
 *       readiness: {green: 0.90, amber: 0.70}
 *     feeds:                           # expected report interval in seconds; a key ending in
 *       "mti:status": 30               # ':' is a prefix matching every feed of that kind
 *       "mavlink:": 1
 *       "cot:": 5
 *     feedSilenceMultiplier: 3         # outage after this many missed intervals (Q18)
 *     requiredFeeds: "mti:status"      # in outage from startup until first seen
 *     controlTopic: "/kpi/control"
 *     faultInjection: false            # allow control messages to clear the MTI cache
 * </pre>
 */
public final class KpiConfig {

  private static final Map<KpiId, BandDefinition> DEFAULT_BANDS = new EnumMap<>(KpiId.class);
  private static final Map<KpiId, String> BAND_KEYS = new EnumMap<>(KpiId.class);

  static {
    DEFAULT_BANDS.put(KpiId.READINESS, new BandDefinition(true, 0.90, 0.70));
    DEFAULT_BANDS.put(KpiId.COVERAGE, new BandDefinition(true, 0.95, 0.80));
    DEFAULT_BANDS.put(KpiId.CLASSIFICATION_TRUST, new BandDefinition(true, 0.95, 0.90));
    DEFAULT_BANDS.put(KpiId.POSITION_STALE, new BandDefinition(false, 0.05, 0.20));
    DEFAULT_BANDS.put(KpiId.UNKNOWN_DEGRADED, new BandDefinition(false, 0.05, 0.20));
    DEFAULT_BANDS.put(KpiId.MTI_STALE, new BandDefinition(false, 0.05, 0.20));
    BAND_KEYS.put(KpiId.READINESS, "readiness");
    BAND_KEYS.put(KpiId.COVERAGE, "coverage");
    BAND_KEYS.put(KpiId.CLASSIFICATION_TRUST, "classificationTrust");
    BAND_KEYS.put(KpiId.POSITION_STALE, "positionStale");
    BAND_KEYS.put(KpiId.UNKNOWN_DEGRADED, "unknownDegraded");
    BAND_KEYS.put(KpiId.MTI_STALE, "mtiStale");
  }

  public enum EligibilityMode {
    ALL,
    LIST,
    ANNOUNCED,
    LIST_AND_ANNOUNCED;

    boolean usesList() {
      return this == LIST || this == LIST_AND_ANNOUNCED;
    }

    boolean usesAnnounced() {
      return this == ANNOUNCED || this == LIST_AND_ANNOUNCED;
    }
  }

  private final Duration evaluationInterval;
  private final Set<TwinType> twinTypes;
  private final Duration positionMaxAge;
  private final Duration mtiMaxAge;
  private final Duration recoveryHold;
  private final Duration rosterAdminTimeout;
  private final EligibilityMode eligibilityMode;
  private final Set<String> eligibleUids;
  private final Set<String> rosterUids;
  private final Map<String, String> expectedClassifications;
  private final Map<KpiId, BandDefinition> bands;
  private final Map<String, Duration> feedIntervals;
  private final double feedSilenceMultiplier;
  private final Set<String> requiredFeeds;
  private final String controlTopic;
  private final boolean faultInjection;
  private final Duration runTimeout;
  private final Duration rebuildTimeout;
  private final double rebuildTargetFraction;
  private final Duration snapshotInterval;
  private final Path dataDirectory;

  private KpiConfig(ConfigurationProperties props) {
    evaluationInterval = Duration.ofMillis(Math.max(100L, props.getLongProperty("evaluationIntervalMillis", 1000L)));
    twinTypes = EnumSet.noneOf(TwinType.class);
    for (String name : list(props.get("twinTypes"), List.of(TwinType.DRONE.name()))) {
      twinTypes.add(TwinType.valueOf(name.toUpperCase(Locale.ROOT)));
    }
    positionMaxAge = Duration.ofSeconds(props.getLongProperty("positionMaxAgeSeconds", 30L));
    mtiMaxAge = Duration.ofSeconds(props.getLongProperty("mtiMaxAgeSeconds", 120L));
    recoveryHold = Duration.ofSeconds(props.getLongProperty("recoveryHoldSeconds", 15L));
    rosterAdminTimeout = Duration.ofSeconds(props.getLongProperty("rosterAdminTimeoutSeconds", 3600L));
    eligibilityMode = EligibilityMode.valueOf(props.getProperty("eligibilityMode", "all").trim().toUpperCase(Locale.ROOT));
    eligibleUids = new LinkedHashSet<>(list(props.get("eligibleUids"), List.of()));
    rosterUids = new LinkedHashSet<>(list(props.get("rosterUids"), List.of()));
    expectedClassifications = new LinkedHashMap<>();
    map(props.get("expectedClassifications")).forEach((uid, type) -> expectedClassifications.put(uid, String.valueOf(type)));

    bands = new EnumMap<>(DEFAULT_BANDS);
    Map<String, Object> bandConfig = map(props.get("bands"));
    for (Map.Entry<KpiId, String> entry : BAND_KEYS.entrySet()) {
      Map<String, Object> band = map(bandConfig.get(entry.getValue()));
      if (!band.isEmpty()) {
        BandDefinition defaults = DEFAULT_BANDS.get(entry.getKey());
        bands.put(entry.getKey(), new BandDefinition(defaults.higherIsBetter(),
            number(band.get("green"), defaults.green()), number(band.get("amber"), defaults.amber())));
      }
    }

    feedIntervals = new LinkedHashMap<>();
    Map<String, Object> feeds = map(props.get("feeds"));
    if (feeds.isEmpty()) {
      feeds = Map.of("mti:status", 30, "mavlink:", 1, "cot:", 5);
    }
    feeds.forEach((feed, seconds) -> feedIntervals.put(feed, Duration.ofMillis((long) (number(seconds, 1) * 1000))));
    feedSilenceMultiplier = props.getDoubleProperty("feedSilenceMultiplier", 3.0);
    requiredFeeds = new LinkedHashSet<>(list(props.get("requiredFeeds"), List.of()));
    controlTopic = props.getProperty("controlTopic", "/kpi/control");
    faultInjection = props.getBooleanProperty("faultInjection", false);
    runTimeout = Duration.ofSeconds(props.getLongProperty("runTimeoutSeconds", 1800L));
    rebuildTimeout = Duration.ofSeconds(props.getLongProperty("rebuildTimeoutSeconds", 1800L));
    rebuildTargetFraction = props.getDoubleProperty("rebuildTargetFraction", 0.95);
    snapshotInterval = Duration.ofSeconds(Math.max(1L, props.getLongProperty("snapshotIntervalSeconds", 10L)));
    String directory = props.getProperty("dataDirectory");
    dataDirectory = directory == null || directory.isBlank()
        ? Path.of(MapsEnvironment.getMapsData(), "kpi")
        : Path.of(directory);
  }

  public static KpiConfig from(ConfigurationProperties props) {
    return new KpiConfig(props == null ? new ConfigurationProperties() : props);
  }

  /** @return the band of a KPI, or {@code null} for diagnostic KPIs without one. */
  public BandDefinition band(KpiId kpi) {
    return bands.get(kpi);
  }

  /**
   * Expected report interval of a feed: an exact key wins, otherwise the longest matching prefix
   * key (ending in ':'). {@code null} if the feed is not monitored.
   */
  public Duration feedInterval(String feed) {
    Duration exact = feedIntervals.get(feed);
    if (exact != null) {
      return exact;
    }
    String bestPrefix = null;
    for (String key : feedIntervals.keySet()) {
      if (key.endsWith(":") && feed.startsWith(key) && (bestPrefix == null || key.length() > bestPrefix.length())) {
        bestPrefix = key;
      }
    }
    return bestPrefix == null ? null : feedIntervals.get(bestPrefix);
  }

  //<editor-fold desc="Getters">
  public Duration getEvaluationInterval() {
    return evaluationInterval;
  }

  public Set<TwinType> getTwinTypes() {
    return Collections.unmodifiableSet(twinTypes);
  }

  public Duration getPositionMaxAge() {
    return positionMaxAge;
  }

  public Duration getMtiMaxAge() {
    return mtiMaxAge;
  }

  public Duration getRecoveryHold() {
    return recoveryHold;
  }

  public Duration getRosterAdminTimeout() {
    return rosterAdminTimeout;
  }

  public EligibilityMode getEligibilityMode() {
    return eligibilityMode;
  }

  public Set<String> getEligibleUids() {
    return Collections.unmodifiableSet(eligibleUids);
  }

  public Set<String> getRosterUids() {
    return Collections.unmodifiableSet(rosterUids);
  }

  public Map<String, String> getExpectedClassifications() {
    return Collections.unmodifiableMap(expectedClassifications);
  }

  public double getFeedSilenceMultiplier() {
    return feedSilenceMultiplier;
  }

  public Set<String> getRequiredFeeds() {
    return Collections.unmodifiableSet(requiredFeeds);
  }

  public String getControlTopic() {
    return controlTopic;
  }

  public boolean isFaultInjection() {
    return faultInjection;
  }

  public Duration getRunTimeout() {
    return runTimeout;
  }

  public Duration getRebuildTimeout() {
    return rebuildTimeout;
  }

  public double getRebuildTargetFraction() {
    return rebuildTargetFraction;
  }

  public Duration getSnapshotInterval() {
    return snapshotInterval;
  }

  public Path getDataDirectory() {
    return dataDirectory;
  }
  //</editor-fold>

  private static List<String> list(Object value, List<String> defaults) {
    if (value == null) {
      return defaults;
    }
    List<String> result = new ArrayList<>();
    if (value instanceof Collection<?> collection) {
      for (Object item : collection) {
        addTrimmed(result, String.valueOf(item));
      }
    } else {
      for (String item : String.valueOf(value).split(",")) {
        addTrimmed(result, item);
      }
    }
    return result;
  }

  private static void addTrimmed(List<String> result, String item) {
    String trimmed = item.trim();
    if (!trimmed.isEmpty()) {
      result.add(trimmed);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object value) {
    if (value instanceof ConfigurationProperties properties) {
      return properties.getMap();
    }
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Map.of();
  }

  private static double number(Object value, double defaultValue) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value != null) {
      try {
        return Double.parseDouble(String.valueOf(value).trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("not a number: " + value, e);
      }
    }
    return defaultValue;
  }
}
