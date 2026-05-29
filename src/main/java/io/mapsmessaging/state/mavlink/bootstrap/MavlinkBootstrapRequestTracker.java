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

package io.mapsmessaging.state.mavlink.bootstrap;

import java.time.Duration;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MavlinkBootstrapRequestTracker {

  private DroneTwinMissingState missingState;
  private int requestCount;
  private Instant firstRequestedAt;
  private Instant lastRequestedAt;
  private boolean timedOut;

  public MavlinkBootstrapRequestTracker(DroneTwinMissingState missingState) {
    this.missingState = missingState;
  }

  public void markRequested(Instant now) {
    if (firstRequestedAt == null) {
      firstRequestedAt = now;
    }
    lastRequestedAt = now;
    requestCount++;
  }

  public boolean canRetry(Instant now, Duration retryInterval, int maximumRetries) {
    if (timedOut) {
      return false;
    }

    if (requestCount >= maximumRetries) {
      return false;
    }

    if (lastRequestedAt == null) {
      return true;
    }

    return Duration.between(lastRequestedAt, now).compareTo(retryInterval) >= 0;
  }

  public boolean hasTimedOut(Instant now, Duration timeout) {
    if (timedOut) {
      return true;
    }

    if (firstRequestedAt == null) {
      return false;
    }

    return Duration.between(firstRequestedAt, now).compareTo(timeout) >= 0;
  }
}