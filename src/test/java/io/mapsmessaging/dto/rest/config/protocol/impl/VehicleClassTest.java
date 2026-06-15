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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleClassTest {

  @Test
  void domainAndSymbolSet_areDefinedForEveryVehicleClass() {
    assertEquals(VehicleDomain.AIR, VehicleClass.UAV.getDomain());
    assertEquals("SymbolSetEnum_AIR", VehicleClass.UAV.getSymbolSet());
    assertEquals(VehicleDomain.SURFACE, VehicleClass.USV.getDomain());
    assertEquals("SymbolSetEnum_SEA_SURFACE", VehicleClass.USV.getSymbolSet());
    assertEquals(VehicleDomain.GROUND, VehicleClass.UGV.getDomain());
    assertEquals("SymbolSetEnum_LAND_UNIT", VehicleClass.UGV.getSymbolSet());
    assertEquals(VehicleDomain.UNDERWATER, VehicleClass.UUV.getDomain());
    assertEquals("SymbolSetEnum_SUBSURFACE", VehicleClass.UUV.getSymbolSet());
    assertEquals(VehicleDomain.CONTROL, VehicleClass.GCS.getDomain());
    assertEquals("SymbolSetEnum_CONTROL", VehicleClass.GCS.getSymbolSet());
    assertEquals(VehicleDomain.UNKNOWN, VehicleClass.UNKNOWN.getDomain());
    assertEquals("SymbolSetEnum_UNKNOWN", VehicleClass.UNKNOWN.getSymbolSet());
  }

  @Test
  void fromMavType_mapsKnownAndUnknownValues() {
    assertEquals(VehicleClass.UNKNOWN, VehicleClass.fromMavType(-1));
    assertEquals(VehicleClass.UNKNOWN, VehicleClass.fromMavType(0));
    assertEquals(VehicleClass.UAV, VehicleClass.fromMavType(1));
    assertEquals(VehicleClass.UGV, VehicleClass.fromMavType(5));
    assertEquals(VehicleClass.GCS, VehicleClass.fromMavType(6));
    assertEquals(VehicleClass.UAV, VehicleClass.fromMavType(9));
    assertEquals(VehicleClass.UGV, VehicleClass.fromMavType(10));
    assertEquals(VehicleClass.USV, VehicleClass.fromMavType(11));
    assertEquals(VehicleClass.UUV, VehicleClass.fromMavType(12));
    assertEquals(VehicleClass.UNKNOWN, VehicleClass.fromMavType(13));
  }
}
