package io.mapsmessaging.dto.rest.config.protocol.impl;

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