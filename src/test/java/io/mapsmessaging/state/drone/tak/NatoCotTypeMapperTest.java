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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NatoCotTypeMapperTest {

  private final NatoCotTypeMapper mapper = new NatoCotTypeMapper();

  @Test
  void mapsNumericSidcToFullCotHierarchy() {
    assertEquals("a-f-G-U-C-I", mapper.fromNumericSidc("10031000001211000000"));
  }

  @Test
  void mapsStanagDescriptionToFullLandUnitCotHierarchy() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_LAND_UNIT",
            "entity", "12",
            "entity_type", "11",
            "entity_subtype", "00",
            "sector_1", "00",
            "sector_2", "00");

    assertEquals("a-f-G-U-C-I", mapper.fromDescription(description));
  }

  @Test
  void mapsUnmannedAircraftInsteadOfUtilityAircraft() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_AIR",
            "entity", "11",
            "entity_type", "03",
            "entity_subtype", "00");

    assertEquals("a-f-A-M-F-Q", mapper.fromDescription(description));
  }

  @Test
  void mapsMilitaryUnmannedSurfaceVehicle() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_SEA_SURFACE",
            "entity", "12",
            "entity_type", "07",
            "entity_subtype", "00",
            "sector_1", "00",
            "sector_2", "00");

    assertEquals("a-f-S-C-U", mapper.fromDescription(description));
  }

  @Test
  void genericMilitarySeaSurfaceFallsBackToDimension() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_SEA_SURFACE",
            "entity", "11",
            "entity_type", "00",
            "entity_subtype", "00");

    assertEquals("a-f-S", mapper.fromDescription(description));
  }

  @Test
  void mapsSeaSurfaceClassification() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_SEA_SURFACE",
            "entity", "14",
            "entity_type", "01",
            "entity_subtype", "00");

    assertEquals("a-f-S-X-M", mapper.fromDescription(description));
  }

  @Test
  void preservesHostileAffiliation() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_HOSTILE",
            "symbol_set", "SymbolSetEnum_LAND_UNIT",
            "entity", "12",
            "entity_type", "11",
            "entity_subtype", "00");

    assertEquals("a-h-G-U-C-I", mapper.fromDescription(description));
  }

  @Test
  void fallsBackToAffiliationAndDimensionWhenNoLegacyFunctionExists() {
    Map<String, Object> description =
        Map.of(
            "standard_identity", "StandardIdentityEnum_FRIEND",
            "symbol_set", "SymbolSetEnum_LAND_UNIT",
            "entity", "99",
            "entity_type", "99",
            "entity_subtype", "99");

    assertEquals("a-f-G", mapper.fromDescription(description));
  }

  @Test
  void acceptsDirectNumericSidcFromDescription() {
    assertEquals("a-f-G-U-C-I", mapper.fromDescription(Map.of("sidc", "10031000001211000000")));
  }

  @Test
  void returnsNullWhenNatoClassificationIsIncomplete() {
    assertNull(mapper.fromDescription(Map.of("standard_identity", "StandardIdentityEnum_FRIEND")));
  }

  @Test
  void returnsNullForInvalidNumericSidc() {
    assertNull(mapper.fromNumericSidc("not-a-sidc"));
  }
}
