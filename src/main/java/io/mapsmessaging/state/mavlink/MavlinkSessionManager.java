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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.dto.rest.config.twin.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.drone.core.TwinManager;

import java.io.IOException;

public class MavlinkSessionManager {

  private final MavlinkStateSubscriber stateSubscriber;

  public MavlinkSessionManager(TwinManager twinManager, MavlinkTwinConfigDTO config) throws IOException {
    stateSubscriber = new MavlinkStateSubscriber(twinManager, config);
  }

  public void start() throws IOException{
    stateSubscriber.start();
  }

  public void stop() throws IOException{
    stateSubscriber.stop();
  }
}
