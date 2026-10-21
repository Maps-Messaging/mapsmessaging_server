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

import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationRegistryTest {

  private final TakEventMapper mapper = new TakEventMapper();
  private final CotEventPolicy policy = new CotEventPolicy();

  @AfterEach
  void clear() {
    ClassificationRegistry.clearForTest();
    MtiStatusRegistry.setDelegate(null);
  }

  private DroneTwin twin(String twinId, VehicleClass vehicleClass) {
    DroneTwin twin = new DroneTwin(twinId);
    twin.setVehicleClass(vehicleClass);
    twin.setGeoPosition(new GeoPosition(-33.0d, 151.0d, 0.0d, null));
    twin.setLastSeenAt(Instant.parse("2026-10-01T10:00:00Z"));
    return twin;
  }

  private ClassificationRegistry.Classification compose(DroneTwin twin) {
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    policy.apply(event, twin, null, null);
    return ClassificationRegistry.get(twin.getTwinId());
  }

  @Test
  void resolvedVehicleClass_isRecordedAsResolved() {
    ClassificationRegistry.Classification classification = compose(twin("uav-1", VehicleClass.UAV));

    assertEquals(ClassificationRegistry.Outcome.RESOLVED, classification.outcome());
    assertEquals("a-f-A-M-F-Q", classification.baseCotType());
  }

  @Test
  void keptOriginalCotType_isAFallback_notAFailure() {
    DroneTwin ingested = twin("partner-1", null);
    ingested.getAttributes().put(CotToTwinMapper.ORIGINAL_COT_TYPE_ATTRIBUTE, "a-h-A-M-F-R");

    assertEquals(ClassificationRegistry.Outcome.FALLBACK, compose(ingested).outcome());
  }

  @Test
  void genericType_isTheHardFailure_evenWhenKeptFromCot() {
    assertEquals(ClassificationRegistry.Outcome.UNRESOLVED, compose(twin("relayed-1", null)).outcome());

    DroneTwin genericIngest = twin("partner-2", null);
    genericIngest.getAttributes().put(CotToTwinMapper.ORIGINAL_COT_TYPE_ATTRIBUTE, "a-u-G");
    assertEquals(ClassificationRegistry.Outcome.UNRESOLVED, compose(genericIngest).outcome());
  }

  @Test
  void baseType_isRecordedBeforeTheMtiAffiliationOverride() {
    MtiStatusRegistry.setDelegate(twinId -> new MtiLookupResult("u", null, "MTI: unknown", null, null));

    assertEquals("a-f-A-M-F-Q", compose(twin("uav-2", VehicleClass.UAV)).baseCotType());
  }

  @Test
  void removal_doesNotOverwriteTheLastClassification() {
    DroneTwin twin = twin("uav-3", VehicleClass.UAV);
    compose(twin);
    twin.setVehicleClass(null);
    TakEvent removal = mapper.map(twin, new TwinUpdateContext());
    policy.applyRemoval(removal, twin, null, null);

    assertEquals(ClassificationRegistry.Outcome.RESOLVED, ClassificationRegistry.get("uav-3").outcome());
  }

  @Test
  void isGeneric_dimensionOnlyOrBlank() {
    assertTrue(ClassificationRegistry.isGeneric("a-f-A"));
    assertTrue(ClassificationRegistry.isGeneric(""));
    assertTrue(ClassificationRegistry.isGeneric(null));
    assertFalse(ClassificationRegistry.isGeneric("a-f-S-C-U"));
  }

  @Test
  void forget_removesTheAsset() {
    compose(twin("uav-4", VehicleClass.UAV));
    ClassificationRegistry.forget("uav-4");

    assertNull(ClassificationRegistry.get("uav-4"));
  }
}
