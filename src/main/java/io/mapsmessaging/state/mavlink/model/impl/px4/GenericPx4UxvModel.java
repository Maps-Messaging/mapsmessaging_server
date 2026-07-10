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

package io.mapsmessaging.state.mavlink.model.impl.px4;

import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.mapsmessaging.state.mavlink.model.impl.AbstractMissionUxvModel;
import java.util.Objects;
import java.util.Set;

public abstract class GenericPx4UxvModel extends AbstractMissionUxvModel {

  protected GenericPx4UxvModel(String modelName, UxvVehicleType vehicleType, Set<UxvOperation> supportedOperations) {
    super(modelName, vehicleType, supportedOperations);
  }

  public UxvModelCommandSet returnToHome(UxvCommandContext context) {
    Objects.requireNonNull(context, "context must not be null");
    return UxvModelCommandSet.of(
        UxvOperation.RETURN_TO_HOME,
        getModelName(),
        MavlinkCommandLongFactory.returnToLaunch(context.targetSystem(), context.targetComponent(), context.sequence()));
  }

  @Override
  public UxvModelCommandSet stop(UxvCommandContext context) {
    return pauseVehicle(context);
  }
}
