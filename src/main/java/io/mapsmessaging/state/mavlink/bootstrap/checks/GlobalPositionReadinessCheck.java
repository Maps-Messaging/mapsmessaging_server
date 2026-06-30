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

package io.mapsmessaging.state.mavlink.bootstrap.checks;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinMissingState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluation;

import java.time.Duration;
import java.time.Instant;

public class GlobalPositionReadinessCheck implements DroneTwinReadinessCheck {

  private final Duration positionFreshness;

  public GlobalPositionReadinessCheck(Duration positionFreshness) {
    this.positionFreshness = positionFreshness;
  }

  @Override
  public void evaluate(
      DroneTwin droneTwin,
      DroneTwinReadinessEvaluation evaluation
  ) {
    GeoPosition geoPosition = droneTwin.getGeoPosition();

    if (geoPosition == null
        || geoPosition.getLatitude() == null
        || geoPosition.getLongitude() == null) {
      evaluation.blocking(DroneTwinMissingState.MISSING_GLOBAL_POSITION);
      return;
    }

    if (isStale(droneTwin.getNavigationUpdatedAt(), evaluation.getEvaluatedAt())) {
      evaluation.blocking(DroneTwinMissingState.STALE_POSITION);
    }
  }

  private boolean isStale(Instant timestamp, Instant now) {
    if (timestamp == null) {
      return true;
    }

    return Duration.between(timestamp, now).compareTo(positionFreshness) > 0;
  }
}