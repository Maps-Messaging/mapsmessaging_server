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

package io.mapsmessaging.state;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfigDTO;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.mavlink.MavlinkStateSubscriber;
import io.mapsmessaging.utilities.Lifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_TWIN_MANAGER_START_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_TWIN_MANAGER_STOP_FAILED;

public class MavlinkTwinManager implements Lifecycle {

  private final Logger logger = LoggerFactory.getLogger(MavlinkTwinManager.class);

  private final List<MavlinkStateSubscriber> mavlinkSessionManagers = new ArrayList<>();

  public MavlinkTwinManager(TwinManager twinManager, DroneInfoRegistry registry, TwinManagerConfigDTO config) {
    if (config != null) {
      for (MavlinkTwinConfigDTO mavlinkConfig : config.getMavlink()) {
        mavlinkSessionManagers.add(new MavlinkStateSubscriber(twinManager, mavlinkConfig, registry));
      }
    }
  }

  @Override
  public void start() {
    for (MavlinkStateSubscriber mavlinkSessionManager : mavlinkSessionManagers) {
      try {
        mavlinkSessionManager.start();
      } catch (IOException e) {
        logger.log(MAVLINK_TWIN_MANAGER_START_FAILED, e.getMessage());
      }
    }
  }

  @Override
  public void stop() {
    for (MavlinkStateSubscriber mavlinkSessionManager : mavlinkSessionManagers) {
      try {
        mavlinkSessionManager.stop();
      } catch (IOException e) {
        logger.log(MAVLINK_TWIN_MANAGER_STOP_FAILED, e.getMessage());
      }
    }
  }
}