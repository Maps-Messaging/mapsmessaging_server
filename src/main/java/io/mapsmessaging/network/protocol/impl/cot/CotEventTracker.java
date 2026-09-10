/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.network.protocol.impl.cot.CotEchoSuppressor.CotEventInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

final class CotEventTracker {

  private final String origin;
  private final int maximumHopCount;
  private final int cacheSize;
  private final Duration cacheTtl;
  private final Duration clockSkew;
  private final int maximumTrackedUids;
  private final Clock clock;
  private final LinkedHashMap<String, Instant> outboundFingerprints;
  private final LinkedHashMap<String, Instant> inboundFingerprints;
  private final LinkedHashMap<String, Instant> latestByUid;

  CotEventTracker(
      String origin,
      int maximumHopCount,
      int cacheSize,
      Duration cacheTtl,
      Duration clockSkew,
      int maximumTrackedUids,
      Clock clock) {
    this.origin = origin;
    this.maximumHopCount = requirePositive(maximumHopCount, "maximumHopCount");
    this.cacheSize = requirePositive(cacheSize, "cacheSize");
    this.maximumTrackedUids = requirePositive(maximumTrackedUids, "maximumTrackedUids");
    if (cacheTtl.isZero() || cacheTtl.isNegative()) {
      throw new IllegalArgumentException("cacheTtl must be positive");
    }
    if (clockSkew.isNegative()) {
      throw new IllegalArgumentException("clockSkew must not be negative");
    }
    this.cacheTtl = cacheTtl;
    this.clockSkew = clockSkew;
    this.clock = clock;
    outboundFingerprints = new LinkedHashMap<>(16, 0.75f, true);
    inboundFingerprints = new LinkedHashMap<>(16, 0.75f, true);
    latestByUid = new LinkedHashMap<>(16, 0.75f, true);
  }

  synchronized Decision evaluateInbound(CotEventInfo event) {
    Instant now = clock.instant();
    purgeExpired(outboundFingerprints, now);
    purgeExpired(inboundFingerprints, now);
    if (event.origins().contains(origin)) {
      return Decision.DIRECT_ECHO;
    }
    if (event.hopCount() > maximumHopCount) {
      return Decision.HOP_LIMIT;
    }
    if (outboundFingerprints.containsKey(event.fingerprint())) {
      return Decision.SEMANTIC_ECHO;
    }
    if (inboundFingerprints.containsKey(event.fingerprint())) {
      return Decision.DUPLICATE;
    }
    if (event.stale().plus(clockSkew).isBefore(now)) {
      return Decision.EXPIRED;
    }
    if (event.start().minus(clockSkew).isAfter(now)) {
      return Decision.NOT_STARTED;
    }
    Instant latest = latestByUid.get(event.uid());
    if (latest != null && event.time().isBefore(latest)) {
      return Decision.OLDER_UPDATE;
    }
    putBounded(inboundFingerprints, event.fingerprint(), now, cacheSize);
    if (latest == null || event.time().isAfter(latest)) {
      putBounded(latestByUid, event.uid(), event.time(), maximumTrackedUids);
    }
    return Decision.ACCEPT;
  }

  synchronized Decision evaluateOutbound(CotEventInfo event) {
    Instant now = clock.instant();
    purgeExpired(outboundFingerprints, now);
    if (event.origins().contains(origin)) {
      return Decision.DIRECT_ECHO;
    }
    if (event.hopCount() >= maximumHopCount) {
      return Decision.HOP_LIMIT;
    }
    if (event.stale().plus(clockSkew).isBefore(now)) {
      return Decision.EXPIRED;
    }
    if (event.start().minus(clockSkew).isAfter(now)) {
      return Decision.NOT_STARTED;
    }
    return Decision.ACCEPT;
  }

  synchronized void rememberOutbound(CotEventInfo event) {
    Instant now = clock.instant();
    purgeExpired(outboundFingerprints, now);
    putBounded(outboundFingerprints, event.fingerprint(), now, cacheSize);
  }

  private void purgeExpired(LinkedHashMap<String, Instant> cache, Instant now) {
    Instant threshold = now.minus(cacheTtl);
    Iterator<Map.Entry<String, Instant>> iterator = cache.entrySet().iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getValue().isBefore(threshold)) {
        iterator.remove();
      }
    }
  }

  private static void putBounded(
      LinkedHashMap<String, Instant> cache,
      String key,
      Instant value,
      int maximumSize) {
    cache.put(key, value);
    while (cache.size() > maximumSize) {
      Iterator<String> iterator = cache.keySet().iterator();
      iterator.next();
      iterator.remove();
    }
  }

  private static int requirePositive(int value, String name) {
    if (value < 1) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  enum Decision {
    ACCEPT,
    DIRECT_ECHO,
    SEMANTIC_ECHO,
    HOP_LIMIT,
    DUPLICATE,
    EXPIRED,
    NOT_STARTED,
    OLDER_UPDATE
  }
}
