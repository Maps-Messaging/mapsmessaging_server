/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.state.n2k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class N2kTwinUpdaterTest {

  @Test
  void configuredSpecializationIsCopiedToNewTwin() {
    TwinManager twinManager = new TwinManager();
    N2kTwinUpdater updater = new N2kTwinUpdater(twinManager);

    N2KTwinConfig config = new N2KTwinConfig();
    config.setName("RHIB-001");

    DroneInfoDTO droneInfo = new DroneInfoDTO();
    droneInfo.setUuid(UUID.fromString("4e607e59-adcc-5680-a433-6e9ea3d3c895"));
    droneInfo.setSpecialization(
        Map.of(
            "$discriminator", "NodeSpecializationTypeEnum_SURFACE_UNMANNED_SYSTEM",
            "surface_unmanned_system",
            Map.of("app11_vessel_type", "NavalVesselTypeEnum_UNKNOWN")));

    TwinUpdateContext context = new TwinUpdateContext();
    context.setUpdateSource("n2k-test");
    context.setReceivedTime(Instant.parse("2026-09-19T19:00:00Z"));

    updater.updateTwinState(0, new JsonObject(), context, config, droneInfo);

    DroneTwin twin =
        assertInstanceOf(DroneTwin.class, twinManager.getTwin("RHIB-001").orElseThrow());

    assertEquals(
        "NodeSpecializationTypeEnum_SURFACE_UNMANNED_SYSTEM",
        twin.getSpecialization().get("$discriminator"));
    Map<?, ?> body =
        assertInstanceOf(
            Map.class, twin.getSpecialization().get("surface_unmanned_system"));
    assertEquals("NavalVesselTypeEnum_UNKNOWN", body.get("app11_vessel_type"));
  }
}
