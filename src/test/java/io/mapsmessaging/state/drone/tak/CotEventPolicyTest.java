/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    assertEquals("a-n-S-C-U", event.getType());
  }

  @Test
  void nonStandardSourceAffiliationFallsBackToUnknown() {
    DroneTwin twin = twin(VehicleClass.USV);
    twin.getDescription().put("standard_identity", "ENEMY");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.SOURCE);

    policy.apply(event, twin, null, config);

    assertEquals("a-u-S-C-U", event.getType());
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

    assertEquals("a-h-A-M-F-Q", event.getType());
    assertEquals("m-g", event.getHow());
    assertEquals("stanag-drone-1", event.getUid());
    assertEquals("2026-09-12T10:00:45Z", event.getStale());
    assertEquals(25.0d, event.getPoint().getCe());
    assertEquals(30.0d, event.getPoint().getLe());
    assertEquals("BARO", event.getDetail().getPrecisionLocation().getAltsrc());
  }

  @Test
  void copTwinTakesItsDimensionFromTheStanagSymbolSet() {
    // a COP twin: no vehicle class, only the STANAG description
    DroneTwin twin = twin(null);
    twin.getDescription().put("standard_identity", "StandardIdentityEnum_FRIEND");
    twin.getDescription().put("symbol_set", "SymbolSetEnum_SEA_SURFACE");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.SOURCE);

    policy.apply(event, twin, null, config);

    assertEquals("a-f-S", event.getType());
  }

  @Test
  void copTwinWithoutIdentityIsUnknownNotFriendly() {
    DroneTwin twin = twin(null);
    twin.getDescription().put("symbol_set", "AIR");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.SOURCE);

    policy.apply(event, twin, null, config);

    assertEquals("a-u-A", event.getType());
  }

  @Test
  void withoutConfigurationTheStanagClassificationIsTranslated() {
    // no cot configuration at all: affiliation and dimension come from the CATL description
    DroneTwin partner = twin(null);
    partner.getDescription().put("standard_identity", "StandardIdentityEnum_NEUTRAL");
    partner.getDescription().put("symbol_set", "SymbolSetEnum_SEA_SURFACE");
    TakEvent event = mapper.map(partner, new TwinUpdateContext());
    policy.apply(event, partner, null, null);
    assertEquals("a-n-S", event.getType());

    DroneTwin unclassified = twin(null);
    unclassified.getDescription().put("symbol_set", "AIR");
    TakEvent unknown = mapper.map(unclassified, new TwinUpdateContext());
    policy.apply(unknown, unclassified, null, null);
    assertEquals("a-u-A", unknown.getType());
  }

  @Test
  void aVehicleWithoutAStanagDescriptionStaysFriendly() {
    // a MAVLink USV attached to this server, no description configured
    DroneTwin own = twin(VehicleClass.USV);
    TakEvent event = mapper.map(own, new TwinUpdateContext());
    policy.apply(event, own, null, null);
    assertEquals("a-f-S-C-U", event.getType());
  }

  @Test
  void aConfiguredAffiliationStillOverridesTheSource() {
    DroneTwin twin = twin(null);
    twin.getDescription().put("standard_identity", "HOSTILE");
    twin.getDescription().put("symbol_set", "SEA_SURFACE");
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.FRIENDLY);
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    policy.apply(event, twin, null, config);
    assertEquals("a-f-S", event.getType());
  }

  @Test
  void takvIsDroppedWhenTheNamespaceSaysSo() {
    DroneTwin twin = twin(VehicleClass.USV);
    CotConfigDTO config = new CotConfigDTO();

    TakEvent kept = mapper.map(twin, new TwinUpdateContext());
    policy.apply(kept, twin, null, config);
    assertNotNull(kept.getDetail().getTakv());

    config.setPublishTakv(false);
    TakEvent dropped = mapper.map(twin, new TwinUpdateContext());
    policy.apply(dropped, twin, null, config);
    assertNull(dropped.getDetail().getTakv());
    assertEquals("drone-1", dropped.getUid());
  }

  @Test
  void removalKeepsImmediateStaleTime() {
    DroneTwin twin = twin(VehicleClass.UUV);
    TakEvent event = mapper.mapRemoval(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.NEUTRAL);
    config.setStaleTimeoutMillis(120_000L);

    policy.applyRemoval(event, twin, null, config);

    assertEquals("a-n-U-S-U", event.getType());
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
        // the CoT type table's entries for unmanned vehicles (io.mapsmessaging:cot)
        Arguments.of(VehicleClass.UAV, "a-f-A-M-F-Q"),
        Arguments.of(VehicleClass.USV, "a-f-S-C-U"),
        Arguments.of(VehicleClass.UGV, "a-f-G-U-C-V-U"),
        Arguments.of(VehicleClass.UUV, "a-f-U-S-U"),
        Arguments.of(VehicleClass.GCS, "a-f-G-U-C"),
        Arguments.of(VehicleClass.UNKNOWN, "a-f-X"));
  }
}