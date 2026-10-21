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
import java.util.EnumMap;
import java.util.Map;

/**
 * Displayed band of one KPI with asymmetric persistence (IC26 KPI clarification Q9): a worse band
 * is shown immediately, a better band only once the raw value has stayed at least that good for
 * the hold time. The improvement is dated to the start of that stable period, not to the end of
 * the hold, so recovery timings are not inflated by the anti-flicker delay.
 */
final class BandTracker {

  /** A change of the displayed band. {@code effectiveAt} is when the raw value reached it. */
  record Transition(Band from, Band to, Instant effectiveAt) {
  }

  private final Duration hold;
  private final Map<Band, Instant> atLeastSince = new EnumMap<>(Band.class);
  private Band raw = Band.NO_DATA;
  private Band displayed = Band.NO_DATA;
  private Instant displayedSince;
  private Instant notRedSince;
  private Instant greenSince;

  BandTracker(Duration hold) {
    this.hold = hold;
  }

  /** @return the displayed band transition this update caused, or {@code null} if none. */
  Transition update(Band newRaw, Instant now) {
    raw = newRaw;
    trackStability(newRaw, now);
    if (notRedSince == null && displayed != Band.RED) {
      notRedSince = now;
    }

    if (newRaw == Band.NO_DATA || displayed == Band.NO_DATA) {
      // Entering or leaving "no data" is not a degradation or recovery; show it as it is.
      return change(newRaw, now);
    }
    if (newRaw.isWorseThan(displayed)) {
      return change(newRaw, now);
    }
    // Best band (better than displayed) that the raw value has held for the whole hold time.
    for (Band candidate : new Band[]{Band.GREEN, Band.AMBER}) {
      Instant since = atLeastSince.get(candidate);
      if (displayed.isWorseThan(candidate) && since != null && !since.plus(hold).isAfter(now)) {
        return change(candidate, since);
      }
    }
    return null;
  }

  private void trackStability(Band newRaw, Instant now) {
    for (Band band : new Band[]{Band.GREEN, Band.AMBER, Band.RED}) {
      boolean atLeast = newRaw != Band.NO_DATA && !newRaw.isWorseThan(band);
      if (!atLeast) {
        atLeastSince.remove(band);
      } else {
        atLeastSince.putIfAbsent(band, now);
      }
    }
  }

  private Transition change(Band to, Instant effectiveAt) {
    if (to == displayed) {
      return null;
    }
    Transition transition = new Transition(displayed, to, effectiveAt);
    if (to == Band.RED) {
      notRedSince = null;
    } else if (displayed == Band.RED || notRedSince == null) {
      notRedSince = effectiveAt;
    }
    greenSince = to == Band.GREEN ? effectiveAt : null;
    displayed = to;
    displayedSince = effectiveAt;
    return transition;
  }

  Band getRaw() {
    return raw;
  }

  Band getDisplayed() {
    return displayed;
  }

  Instant getDisplayedSince() {
    return displayedSince;
  }

  /** When the displayed band last left red, {@code null} while red. */
  Instant getNotRedSince() {
    return notRedSince;
  }

  /** When the displayed band last became green, {@code null} while not green. */
  Instant getGreenSince() {
    return greenSince;
  }
}
