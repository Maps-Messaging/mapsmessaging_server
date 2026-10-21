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

package io.mapsmessaging.state.mavlink.model.impl;

import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.mavlink.model.UnsupportedUxvOperationException;
import io.mapsmessaging.state.mavlink.model.UxvModel;
import io.mapsmessaging.state.mavlink.model.UxvNavigationPlan;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.mapsmessaging.state.mavlink.model.UxvCommandContext;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractUxvModel implements UxvModel {

  private final String modelName;
  private final UxvVehicleType vehicleType;
  private final Set<UxvOperation> supportedOperations;

  protected AbstractUxvModel(String modelName, UxvVehicleType vehicleType, Set<UxvOperation> supportedOperations) {
    this.modelName = Objects.requireNonNull(modelName, "modelName must not be null");
    this.vehicleType = Objects.requireNonNull(vehicleType, "vehicleType must not be null");
    this.supportedOperations = Set.copyOf(Objects.requireNonNull(supportedOperations, "supportedOperations must not be null"));
  }

  protected static Set<UxvOperation> operations(UxvOperation first, UxvOperation... rest) {
    EnumSet<UxvOperation> operations = EnumSet.of(first, rest);
    return Set.copyOf(operations);
  }

  @Override
  public String getModelName() {
    return modelName;
  }

  @Override
  public UxvVehicleType getVehicleType() {
    return vehicleType;
  }

  @Override
  public Set<UxvOperation> getSupportedOperations() {
    return supportedOperations;
  }

  @Override
  public UxvNavigationPlan navigate(UxvCommandContext context, List<GeoPosition> waypoints, Duration duration) {
    throw new UnsupportedUxvOperationException(getModelName(), UxvOperation.NAVIGATE, "Navigation is not supported by model " + getModelName());
  }
}
