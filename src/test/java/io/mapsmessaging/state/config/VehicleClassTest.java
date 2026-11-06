/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VehicleClassTest {

  @Test
  void usesStanagSymbolSetNames() {
    assertEquals("SymbolSetEnum_AIR", VehicleClass.UAV.getSymbolSet());
    assertEquals("SymbolSetEnum_SEA_SURFACE", VehicleClass.USV.getSymbolSet());
    assertEquals("SymbolSetEnum_LAND_UNIT", VehicleClass.UGV.getSymbolSet());
    assertEquals("SymbolSetEnum_SEA_SUBSURFACE", VehicleClass.UUV.getSymbolSet());
    assertEquals("SymbolSetEnum_CONTROL_MEASURE", VehicleClass.GCS.getSymbolSet());
    assertEquals("SymbolSetEnum_UNKNOWN", VehicleClass.UNKNOWN.getSymbolSet());
  }
}
