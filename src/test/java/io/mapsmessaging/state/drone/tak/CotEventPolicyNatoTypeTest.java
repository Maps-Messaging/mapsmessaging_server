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

import io.mapsmessaging.state.config.CotAffiliation;
import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CotEventPolicyNatoTypeTest {

  private final TakEventMapper mapper = new TakEventMapper();
  private final CotEventPolicy policy = new CotEventPolicy();

  @Test
  void preservesNatoClassificationWhenApplyingPolicy() {
    DroneTwin twin = militaryUsv();
    TakEvent event = mapper.map(twin, null);
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.FRIENDLY);

    assertNotNull(event);
    assertEquals("a-f-S-C-U", event.getType());

    policy.apply(event, twin, null, config);

    assertEquals("a-f-S-C-U", event.getType());
  }

  @Test
  void changesOnlyAffiliationWhenPolicyOverridesMappedType() {
    DroneTwin twin = militaryUsv();
    TakEvent event = mapper.map(twin, null);
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.HOSTILE);

    assertNotNull(event);
    assertEquals("a-f-S-C-U", event.getType());

    policy.apply(event, twin, null, config);

    assertEquals("a-h-S-C-U", event.getType());
  }

  private DroneTwin militaryUsv() {
    DroneTwin twin = new DroneTwin("cot-policy-usv");
    twin.setVehicleClass(VehicleClass.USV);
    twin.setGeoPosition(new GeoPosition(38.444, -9.101, 0.0, null));
    twin.setDescription(
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_SEA_SURFACE",
            "entity", "12",
            "entity_type", "07",
            "entity_subtype", "00",
            "sector_1", "00",
            "sector_2", "00"));
    return twin;
  }
}
