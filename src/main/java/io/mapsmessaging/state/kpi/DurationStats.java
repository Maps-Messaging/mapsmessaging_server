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

/**
 * Count, sum, last and max of a recorded duration, plus a count of events that never reached it.
 * Mean = sum / count. Few events are expected per exercise (a handful of fault runs per failure
 * type), so individual values go to the KPI event log rather than a histogram.
 */
final class DurationStats {

  private long count;
  private double sumSeconds;
  private double lastSeconds = -1.0;
  private double maxSeconds = -1.0;
  private long notReachedCount;

  synchronized void record(Duration duration) {
    double seconds = Math.max(0L, duration.toMillis()) / 1000.0;
    count++;
    sumSeconds += seconds;
    lastSeconds = seconds;
    maxSeconds = Math.max(maxSeconds, seconds);
  }

  synchronized void recordNotReached() {
    notReachedCount++;
  }

  synchronized long getCount() {
    return count;
  }

  synchronized double getSumSeconds() {
    return sumSeconds;
  }

  /** -1 if nothing recorded yet. */
  synchronized double getLastSeconds() {
    return lastSeconds;
  }

  /** -1 if nothing recorded yet. */
  synchronized double getMaxSeconds() {
    return maxSeconds;
  }

  synchronized long getNotReachedCount() {
    return notReachedCount;
  }
}
