package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle class (UAV=air, USV=surface, UGV=ground, UUV=underwater, GCS=control).")
public enum VehicleClass {
  UAV,
  USV,
  UGV,
  UUV,
  GCS;

  public VehicleDomain getDomain() {
    return switch (this) {
      case UAV -> VehicleDomain.AIR;
      case USV -> VehicleDomain.SURFACE;
      case UGV -> VehicleDomain.GROUND;
      case UUV -> VehicleDomain.UNDERWATER;
      case GCS -> VehicleDomain.CONTROL;
    };
  }
}
