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

package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TakEventMapperNatoTypeTest {

  private final TakEventMapper mapper = new TakEventMapper();

  @Test
  void natoDescriptionTakesPrecedenceOverVehicleClassFallback() {
    DroneTwin twin = positionedTwin(VehicleClass.UAV);
    twin.setDescription(
        Map.of(
            "standard_identity", "StandardIdentityEnum_HOSTILE",
            "symbol_set", "SymbolSetEnum_LAND_UNIT",
            "entity", "12",
            "entity_type", "11",
            "entity_subtype", "00"));

    TakEvent event = mapper.map(twin, null);

    assertNotNull(event);
    assertEquals("a-h-G-U-C-I", event.getType());
  }

  @Test
  void vehicleClassFallbackIsPreservedWithoutNatoClassification() {
    DroneTwin twin = positionedTwin(VehicleClass.USV);

    TakEvent event = mapper.map(twin, null);

    assertNotNull(event);
    assertEquals("a-f-S-X-M", event.getType());
  }

  private DroneTwin positionedTwin(VehicleClass vehicleClass) {
    DroneTwin twin = new DroneTwin("cot-test");
    twin.setVehicleClass(vehicleClass);
    twin.setGeoPosition(new GeoPosition(38.444, -9.101, 5.0, null));
    return twin;
  }
}
