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
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.*;

@Getter
public class MavlinkBootstrapProfile {

  private static final int ONE_HERTZ_MICROSECONDS = 1_000_000;
  private static final int TWO_HERTZ_MICROSECONDS = 500_000;

  private final Duration retryInterval;
  private final Duration timeout;
  private final int maximumRetries;
  private final Map<DroneTwinMissingState, MavlinkBootstrapRequestDefinition> requestDefinitions;

  public MavlinkBootstrapProfile() {
    this.retryInterval = Duration.ofSeconds(2);
    this.timeout = Duration.ofSeconds(15);
    this.maximumRetries = 3;
    this.requestDefinitions = new EnumMap<>(DroneTwinMissingState.class);

    addRequestMessage(
        DroneTwinMissingState.MISSING_AUTOPILOT_VERSION,
        AUTOPILOT_VERSION
    );

    addRequestMessage(
        DroneTwinMissingState.MISSING_HOME_POSITION,
        HOME_POSITION
    );

    addRequestMessage(
        DroneTwinMissingState.MISSING_BATTERY_STATE,
        BATTERY_STATUS
    );

    addMessageInterval(
        DroneTwinMissingState.MISSING_GLOBAL_POSITION,
        GLOBAL_POSITION_INT,
        TWO_HERTZ_MICROSECONDS
    );

    addMessageInterval(
        DroneTwinMissingState.MISSING_GPS_FIX,
        GPS_RAW_INT,
        ONE_HERTZ_MICROSECONDS
    );

    addMessageInterval(
        DroneTwinMissingState.MISSING_SYSTEM_STATE,
        SYS_STATUS,
        ONE_HERTZ_MICROSECONDS
    );
  }

  private void addRequestMessage(
      DroneTwinMissingState missingState,
      int mavlinkMessageId
  ) {
    requestDefinitions.put(
        missingState,
        MavlinkBootstrapRequestDefinition.requestMessage(mavlinkMessageId)
    );
  }

  private void addMessageInterval(
      DroneTwinMissingState missingState,
      int mavlinkMessageId,
      int intervalMicroseconds
  ) {
    requestDefinitions.put(
        missingState,
        MavlinkBootstrapRequestDefinition.setMessageInterval(
            mavlinkMessageId,
            intervalMicroseconds
        )
    );
  }
}