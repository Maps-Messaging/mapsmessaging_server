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
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.cot.CotManagedPlatformConfigDTO;
import io.mapsmessaging.state.config.cot.CotTwinConfigDTO;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.tak.model.TakContact;
import io.mapsmessaging.state.drone.tak.model.TakDetail;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakPoint;
import io.mapsmessaging.state.drone.tak.model.TakTrack;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CotTwinUpdaterTest {

  @Test
  void managedTelemetry_updatesCanonicalTwinAndConvertsHaeToMsl() {
    TwinManager twinManager = new TwinManager();
    CotTwinUpdater updater = updater(twinManager, -30.0);

    CotUpdateResult result = updater.update("tak-a", event("2026-09-08T10:00:00Z", 38.0), Instant.parse("2026-09-08T10:00:01Z"));
    DroneTwin twin = (DroneTwin) twinManager.getTwin("alpha").orElseThrow();

    assertTrue(result.applied());
    assertEquals(47.1, twin.getGeoPosition().getLatitude());
    assertEquals(8.0, twin.getGeoPosition().getAltitudeMslMeters());
    assertEquals("Falcon", twin.getCallSign());
    assertEquals(12.5, twin.getGroundSpeedMetersPerSecond());
    assertEquals("cot", twinManager.getObservationRegistry()
        .resolve("alpha", "position.latitude", Instant.parse("2026-09-08T10:00:01Z"))
        .orElseThrow().getSourceProtocol());
  }

  @Test
  void lateTelemetry_doesNotRegressTwinPosition() {
    TwinManager twinManager = new TwinManager();
    CotTwinUpdater updater = updater(twinManager, 0.0);

    updater.update("tak-a", event("2026-09-08T10:00:10Z", 20.0), Instant.parse("2026-09-08T10:00:11Z"));
    TakEvent late = event("2026-09-08T10:00:05Z", 10.0);
    late.getPoint().setLat(46.0);
    CotUpdateResult result = updater.update("tak-a", late, Instant.parse("2026-09-08T10:00:12Z"));

    DroneTwin twin = (DroneTwin) twinManager.getTwin("alpha").orElseThrow();
    assertFalse(result.applied());
    assertEquals(47.1, twin.getGeoPosition().getLatitude());
    assertEquals(20.0, twin.getGeoPosition().getAltitudeMslMeters());
  }

  @Test
  void unknownMarker_isObservedButDoesNotCreateControllableTwin() {
    TwinManager twinManager = new TwinManager();
    CotTwinUpdater updater = updater(twinManager, 0.0);
    TakEvent event = event("2026-09-08T10:00:00Z", 20.0);
    event.setUid("unknown");

    CotUpdateResult result = updater.update("tak-a", event, Instant.parse("2026-09-08T10:00:01Z"));

    assertEquals(CotObjectClass.OBSERVED_PLATFORM, result.objectClass());
    assertEquals(0, twinManager.getTwinCount());
  }

  @Test
  void missingAltitudeDatum_omitsAltitudeAndRecordsWarning() {
    TwinManager twinManager = new TwinManager();
    CotTwinUpdater updater = updater(twinManager, null);

    CotUpdateResult result = updater.update("tak-a", event("2026-09-08T10:00:00Z", 38.0), Instant.parse("2026-09-08T10:00:01Z"));
    DroneTwin twin = (DroneTwin) twinManager.getTwin("alpha").orElseThrow();

    assertNull(twin.getGeoPosition().getAltitudeMslMeters());
    assertEquals(1, result.translationWarnings().size());
  }

  @Test
  void preservedExtensions_areAvailableToLaterOutboundMapping() {
    TwinManager twinManager = new TwinManager();
    CotTwinConfigDTO config = configuration(0.0);
    CotTwinUpdater updater = new CotTwinUpdater(
        twinManager, droneInfoRegistry(), new CotIdentityRegistry(config));
    TakEvent inbound = event("2026-09-08T10:00:00Z", 20.0);
    inbound.getDetail().setExtensions(List.of("<vendor-extension value=\"preserved\"/>"));
    updater.update("tak-a", inbound, Instant.parse("2026-09-08T10:00:01Z"));

    TakEvent outbound = new TakEventMapper(
        new CotIdentityRegistry(config), twinManager.getObservationRegistry()).map(
            twinManager.getTwin("alpha").orElseThrow(), null);

    assertEquals(inbound.getDetail().getExtensions(), outbound.getDetail().getExtensions());
  }

  private CotTwinUpdater updater(TwinManager twinManager, Double offset) {
    return new CotTwinUpdater(
        twinManager,
        droneInfoRegistry(),
        new CotIdentityRegistry(configuration(offset)));
  }

  private DroneInfoRegistry droneInfoRegistry() {
    DroneInfoDTO drone = new DroneInfoDTO();
    drone.setName("alpha");
    drone.setUuid(UUID.fromString("5b151a86-c1b9-4e1e-8254-d15437cc4778"));
    drone.setModelName("generic-px4-uav");
    return new DroneInfoRegistry(List.of(drone));
  }

  private CotTwinConfigDTO configuration(Double offset) {
    CotManagedPlatformConfigDTO platform = new CotManagedPlatformConfigDTO();
    platform.setEndpoint("tak-a");
    platform.setUid("vehicle-1");
    platform.setTwinId("alpha");
    platform.setHaeToMslOffsetMeters(offset);
    CotTwinConfigDTO config = new CotTwinConfigDTO();
    config.setManagedPlatforms(List.of(platform));
    return config;
  }

  private TakEvent event(String time, double hae) {
    TakEvent event = new TakEvent();
    event.setUid("vehicle-1");
    event.setType("a-f-A-M-F-U");
    event.setHow("m-g");
    event.setTime(time);
    event.setStart(time);
    event.setStale(Instant.parse(time).plusSeconds(60).toString());
    TakPoint point = new TakPoint();
    point.setLat(47.1);
    point.setLon(8.2);
    point.setHae(hae);
    point.setCe(4.0);
    point.setLe(6.0);
    event.setPoint(point);
    TakDetail detail = new TakDetail();
    TakContact contact = new TakContact();
    contact.setCallsign("Falcon");
    detail.setContact(contact);
    TakTrack track = new TakTrack();
    track.setSpeed(12.5);
    track.setCourse(91.0);
    detail.setTrack(track);
    event.setDetail(detail);
    return event;
  }
}
