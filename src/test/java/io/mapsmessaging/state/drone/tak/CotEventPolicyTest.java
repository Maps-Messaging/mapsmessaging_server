/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.state.config.CotAffiliation;
import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CotEventPolicyTest {

  private final TakEventMapper mapper = new TakEventMapper();
  private final CotEventPolicy policy = new CotEventPolicy();

  @ParameterizedTest
  @MethodSource("vehicleTypes")
  void mapsVehicleClassWithoutAssumingSeaSurface(VehicleClass vehicleClass, String expectedType) {
    DroneTwin twin = twin(vehicleClass);
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.FRIENDLY);

    policy.apply(event, twin, null, config);

    assertEquals(expectedType, event.getType());
  }

  @Test
  void sourceAffiliationUsesStanagIdentity() {
    DroneTwin twin = twin(VehicleClass.USV);
    twin.getDescription().put("standard_identity", "StandardIdentityEnum_NEUTRAL");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.SOURCE);

    policy.apply(event, twin, null, config);

    assertEquals("a-n-S-X-M", event.getType());
  }

  @Test
  void nonStandardSourceAffiliationFallsBackToUnknown() {
    DroneTwin twin = twin(VehicleClass.USV);
    twin.getDescription().put("standard_identity", "ENEMY");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.SOURCE);

    policy.apply(event, twin, null, config);

    assertEquals("a-u-S-X-M", event.getType());
  }

  @Test
  void appliesCotOnlyDefaultsFromNamespaceConfiguration() {
    DroneTwin twin = twin(VehicleClass.UAV);
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.HOSTILE);
    config.setHow("m-g");
    config.setStaleTimeoutMillis(45_000L);
    config.setUidPrefix("stanag-");
    config.setDefaultCircularErrorMeters(25.0d);
    config.setDefaultLinearErrorMeters(30.0d);
    config.setAltitudeSource("BARO");

    policy.apply(event, twin, null, config);

    assertEquals("a-h-A-M-F-U", event.getType());
    assertEquals("m-g", event.getHow());
    assertEquals("stanag-drone-1", event.getUid());
    assertEquals("2026-09-12T10:00:45Z", event.getStale());
    assertEquals(25.0d, event.getPoint().getCe());
    assertEquals(30.0d, event.getPoint().getLe());
    assertEquals("BARO", event.getDetail().getPrecisionLocation().getAltsrc());
  }

  @Test
  void removalKeepsImmediateStaleTime() {
    DroneTwin twin = twin(VehicleClass.UUV);
    TakEvent event = mapper.mapRemoval(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.NEUTRAL);
    config.setStaleTimeoutMillis(120_000L);

    policy.applyRemoval(event, twin, null, config);

    assertEquals("a-n-U-X-M", event.getType());
    assertEquals("2026-09-12T10:00:01Z", event.getStale());
  }

  private DroneTwin twin(VehicleClass vehicleClass) {
    DroneTwin twin = new DroneTwin("drone-1");
    twin.setVehicleClass(vehicleClass);
    twin.setGeoPosition(new GeoPosition(-33.0d, 151.0d, 0.0d, null));
    twin.setLastSeenAt(Instant.parse("2026-09-12T10:00:00Z"));
    return twin;
  }

  private static Stream<Arguments> vehicleTypes() {
    return Stream.of(
        Arguments.of(VehicleClass.UAV, "a-f-A-M-F-U"),
        Arguments.of(VehicleClass.USV, "a-f-S-X-M"),
        Arguments.of(VehicleClass.UGV, "a-f-G-E-V"),
        Arguments.of(VehicleClass.UUV, "a-f-U-X-M"),
        Arguments.of(VehicleClass.GCS, "a-f-G-U-C"),
        Arguments.of(VehicleClass.UNKNOWN, "a-f-X"));
  }
}