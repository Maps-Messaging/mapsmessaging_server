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

package io.mapsmessaging.state.drone;

import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.utilities.Agent;
import lombok.Getter;

public class StateManagerAgent implements Agent {

  @Getter
  private final TwinManager twinManager;

  public StateManagerAgent() {
    twinManager = new TwinManager();
  }

  @Override
  public String getName() {
    return "State Manager";
  }

  @Override
  public String getDescription() {
    return "Manages state of known objects within memory and maintains a digital twin";
  }

  @Override
  public void start() {
    // nothing to do here
  }

  @Override
  public void stop() {
    // nothing to do here
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("Current objects :"+twinManager.getTwinCount());
    status.setStatus(Status.OK);
    return status;
  }
}
