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

  public static VehicleClass fromMavType(int mavType) {
    return switch (mavType) {

      case 6 -> VehicleClass.GCS;

      case 10 -> VehicleClass.UGV;

      case 11 -> VehicleClass.USV;

      case 12 -> VehicleClass.UUV;

      // everything that flies (and things pretending to)
      case 1, 2, 3, 4, 7, 8, 9 -> VehicleClass.UAV;

      // ambiguous / annoying ones
      case 5 -> VehicleClass.UGV; // antenna tracker (ground-based)

      case 0 -> null; // GENERIC → unknown, your call

      default -> VehicleClass.UAV; // future-proof fallback
    };
  }
}
