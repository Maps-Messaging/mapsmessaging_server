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

package io.mapsmessaging.state.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle class (UAV=air, USV=surface, UGV=ground, UUV=underwater, GCS=control).")
public enum VehicleClass {
  UAV,
  USV,
  UGV,
  UUV,
  GCS,
  UNKNOWN;

  public VehicleDomain getDomain() {
    return switch (this) {
      case UAV -> VehicleDomain.AIR;
      case USV -> VehicleDomain.SURFACE;
      case UGV -> VehicleDomain.GROUND;
      case UUV -> VehicleDomain.UNDERWATER;
      case GCS -> VehicleDomain.CONTROL;
      case UNKNOWN -> VehicleDomain.UNKNOWN;
    };
  }

  public String getSymbolSet() {
    return switch (this) {
      case UAV -> "SymbolSetEnum_AIR";
      case USV -> "SymbolSetEnum_SEA_SURFACE";
      case UGV -> "SymbolSetEnum_LAND_UNIT";
      case UUV -> "SymbolSetEnum_SUBSURFACE";
      case GCS -> "SymbolSetEnum_CONTROL";
      case UNKNOWN -> "SymbolSetEnum_UNKNOWN";
    };
  }

  public static VehicleClass fromMavType(int mavType) {
    return switch (mavType) {

      case 6 -> GCS;

      case 10 -> UGV;

      case 11 -> USV;

      case 12 -> UUV;

      // flying things (fixed wing, quad, heli, etc)
      case 1, 2, 3, 4, 7, 8, 9 -> UAV;

      // antenna tracker → ground
      case 5 -> UGV;

      case 0 -> UNKNOWN;

      default -> UNKNOWN;
    };
  }
}