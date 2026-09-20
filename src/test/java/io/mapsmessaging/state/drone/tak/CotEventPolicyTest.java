/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.config.CotAffiliation;
import io.mapsmessaging.state.config.DataProductConfig;
import io.mapsmessaging.state.config.CotConfigDTO;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.tak.model.TakEvent;
import io.mapsmessaging.state.drone.tak.model.TakVideo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CotEventPolicyTest {

  private final TakEventMapper mapper = new TakEventMapper();
  private final CotEventPolicy policy = new CotEventPolicy();

  @AfterEach
  void clearMtiDelegate() {
    MtiStatusRegistry.setDelegate(null);
  }

  @Test
  void preservesOriginalCotTypeForCotIngestedTwin() {
    DroneTwin twin = twin(null);
    twin.getAttributes().put(CotToTwinMapper.ORIGINAL_COT_TYPE_ATTRIBUTE, "a-n-S-C-U");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());

    policy.apply(event, twin, null, null);

    assertEquals("a-n-S-C-U", event.getType());
  }

  @Test
  void mtiCanOverrideAffiliationWithoutChangingClassification() {
    DroneTwin twin = twin(VehicleClass.UAV);
    MtiStatusRegistry.setDelegate(
        twinId -> new MtiLookupResult("u", null, "MTI: unknown", null, null));
    TakEvent event = mapper.map(twin, new TwinUpdateContext());

    policy.apply(event, twin, null, null);

    assertEquals("a-u-A-M-F-Q", event.getType());
  }

  @Test
  void mtiAppliesReadinessAndCyberIconToAirAsset() {
    DroneTwin twin = twin(VehicleClass.UAV);
    MtiStatusRegistry.setDelegate(
        twinId -> new MtiLookupResult(null, -23296, "MTI: mitigate", false, "ddos3_64x64.png"));
    TakEvent event = mapper.map(twin, new TwinUpdateContext());

    policy.apply(event, twin, null, null);

    assertEquals(Boolean.FALSE, event.getDetail().getStatus().getReadiness());
    assertEquals(-23296, event.getDetail().getColorArgb());
    assertEquals(
        "8ed4bdba4a2ff2972685f3420274f87cc8e2d7547ba7262bce94d8991e7f7a9b/cyber_icons/ddos3_64x64.png",
        event.getDetail().getUsericonIconsetPath());

    String xml = new TakXmlSerialiser().toXml(event);
    assertTrue(xml.contains("readiness=\"false\""));
    assertTrue(xml.contains(
        "iconsetpath=\"8ed4bdba4a2ff2972685f3420274f87cc8e2d7547ba7262bce94d8991e7f7a9b/cyber_icons/ddos3_64x64.png\""));
  }

  @Test
  void mavlinkClassificationOverrideReplacesVehicleDerivedClassification() {
    DroneTwin twin = twin(VehicleClass.USV);
    twin.getAttributes().put("cotClassification", "S-C-P");
    TakEvent event = mapper.map(twin, new TwinUpdateContext());
    CotConfigDTO config = new CotConfigDTO();
    config.setAffiliation(CotAffiliation.FRIENDLY);

    policy.apply(event, twin, null, config);

    assertEquals("a-f-S-C-P", event.getType());
  }

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
  void copTwinWithA2525DEntityCodeGetsItsSymbol() {
    // our own boats as they come back on the COP: a USV and a (military) RHIB
    DroneTwin usv = twin(null);
    usv.getDescription().putAll(Map.of("standard_identity", "StandardIdentityEnum_FRIEND",
        "symbol_set", "SymbolSetEnum_SEA_SURFACE", "entity", "12", "entity_type", "07", "entity_subtype", "00"));
    TakEvent usvEvent = mapper.map(usv, new TwinUpdateContext());
    policy.apply(usvEvent, usv, null, null);
    assertEquals("a-f-S-C-U", usvEvent.getType());

    DroneTwin rhib = twin(null);
    rhib.getDescription().putAll(Map.of("standard_identity", "StandardIdentityEnum_FRIEND",
        "symbol_set", "SymbolSetEnum_SEA_SURFACE", "entity", "12", "entity_type", "08", "entity_subtype", "01"));
    TakEvent rhibEvent = mapper.map(rhib, new TwinUpdateContext());
    policy.apply(rhibEvent, rhib, null, null);
    assertEquals("a-f-S-C", rhibEvent.getType());
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
  void aRelayedTrackWithoutItsDescriptionYetIsUnknown() {
    // a COP twin before its NODE_DESCRIPTION arrives: no vehicle class, no description
    DroneTwin relayed = twin(null);
    TakEvent event = mapper.map(relayed, new TwinUpdateContext());
    policy.apply(event, relayed, null, null);
    assertEquals("a-u-X", event.getType());
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


  @Test
  void defaultPolicyAppliesHowErrorsAndPrecisionSources() {
    TakEvent event = new TakEvent();
    event.setPoint(new io.mapsmessaging.state.drone.tak.model.TakPoint());
    event.setDetail(new io.mapsmessaging.state.drone.tak.model.TakDetail());

    policy.apply(event, new DroneTwin("source"), null, null);

    assertEquals("h-g-i-g-o", event.getHow());
    assertEquals(10.0, event.getPoint().getCe(), 0.0);
    assertEquals(15.0, event.getPoint().getLe(), 0.0);
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getAltsrc());
    assertEquals("GPS", event.getDetail().getPrecisionLocation().getGeopointsrc());
  }

  @Test
  void defaultPolicyAdvancesStaleByThirtySeconds() {
    DroneTwin twin = new DroneTwin("drone-default");
    twin.setGeoPosition(new GeoPosition(1.0, 2.0, 3.0, null, null));

    TakEvent event = new TakEvent();
    event.setUid("drone-default");
    event.setType("a-f-A");
    event.setHow("old");
    event.setTime(Instant.parse("2026-09-20T12:00:00Z").toString());
    event.setPoint(new io.mapsmessaging.state.drone.tak.model.TakPoint());
    event.setDetail(new io.mapsmessaging.state.drone.tak.model.TakDetail());

    policy.apply(event, twin, null, null);

    assertEquals("h-g-i-g-o", event.getHow());
    assertEquals("2026-09-20T12:00:30Z", event.getStale());
  }

  @Test
  void nullPolicyInputsAreIgnored() {
    assertDoesNotThrow(() -> policy.apply(null, new DroneTwin("d"), null, null));
    assertDoesNotThrow(() -> policy.apply(new TakEvent(), null, null, null));
  }

  // --- data products on the contact ------------------------------------------------------
  // An asset's external data products (a camera page, a stream) belong where the operator
  // looks: the marker's remarks, which both TAK clients render and linkify.

  @Test
  void aDataProductUriReachesTheMarkersRemarks() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDescriptionString("unmanned surface vehicle");
    usv.setDataProducts(List.of(dataProduct("optical", "https://example.net/optical_view/"),
        dataProduct("thermal", "https://example.net/thermal_view/")));

    TakEvent event = mapper.map(usv, new TwinUpdateContext());

    assertEquals("unmanned surface vehicle | optical=https://example.net/optical_view/"
        + " | thermal=https://example.net/thermal_view/", event.getDetail().getRemarks());
  }

  @Test
  void aDataProductWithoutAUriIsNotRemarkedOn() {
    DroneTwin usv = twin(VehicleClass.USV);
    DataProductConfig noUri = new DataProductConfig();
    noUri.setDescription("sonar log");
    usv.setDataProducts(List.of(noUri, dataProduct("optical", "https://example.net/optical_view/")));

    TakEvent event = mapper.map(usv, new TwinUpdateContext());

    assertEquals("optical=https://example.net/optical_view/", event.getDetail().getRemarks());
  }

  @Test
  void aDataProductFallsBackToItsIdentifierForALabel() {
    DroneTwin usv = twin(VehicleClass.USV);
    DataProductConfig unnamed = new DataProductConfig();
    unnamed.setIdentifier("cam-1");
    unnamed.setUri("https://example.net/optical_view/");
    usv.setDataProducts(List.of(unnamed));

    assertEquals("cam-1=https://example.net/optical_view/",
        mapper.map(usv, new TwinUpdateContext()).getDetail().getRemarks());
  }

  @Test
  void aTwinWithoutDataProductsRemarksExactlyAsBefore() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDescriptionString("unmanned surface vehicle");

    assertEquals("unmanned surface vehicle", mapper.map(usv, new TwinUpdateContext()).getDetail().getRemarks());
  }

  // --- video feeds ------------------------------------------------------------------------
  // A data product TAK's own player can open (RTSP, RTMP, HLS) becomes a __video element, so the
  // marker carries a video icon and plays in the app. Anything else stays a remarks link.

  @Test
  void aPlayableDataProductBecomesAVideoFeedOnTheMarker() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDataProducts(List.of(dataProduct("optical", "rtsp://viewer:secret@example.net:8554/optical_view")));

    TakEvent event = mapper.map(usv, new TwinUpdateContext());

    TakVideo video = event.getDetail().getVideos().get(0);
    // the URL keeps its credentials: the client has nowhere else to carry them
    assertEquals("rtsp://viewer:secret@example.net:8554/optical_view", video.getUrl());
    assertEquals("optical", video.getAlias());
    assertEquals("example.net", video.getAddress());
    assertEquals(8554, video.getPort());
    assertEquals("/optical_view", video.getPath());
    assertEquals("rtsp", video.getProtocol());
    // and it is NOT repeated in the remarks, where the password would be in plain sight
    assertEquals(null, event.getDetail().getRemarks());
  }

  @Test
  void aPageTheTakPlayerCannotOpenStaysALink() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDataProducts(List.of(dataProduct("optical", "https://example.net/optical_view/")));

    TakEvent event = mapper.map(usv, new TwinUpdateContext());

    assertTrue(event.getDetail().getVideos().isEmpty());
    assertEquals("optical=https://example.net/optical_view/", event.getDetail().getRemarks());
  }

  @Test
  void anHlsPlaylistIsPlayableAndAStreamKeepsItsDefaultPort() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDataProducts(List.of(dataProduct("optical", "https://example.net/optical_view/index.m3u8"),
        dataProduct("thermal", "rtsp://example.net/thermal_view")));

    List<TakVideo> videos = mapper.map(usv, new TwinUpdateContext()).getDetail().getVideos();

    assertEquals(2, videos.size());
    assertEquals("https", videos.get(0).getProtocol());
    assertEquals(443, videos.get(0).getPort());
    assertEquals("/optical_view/index.m3u8", videos.get(0).getPath());
    assertEquals(554, videos.get(1).getPort());          // the RTSP default
  }

  @Test
  void aFeedKeepsTheSameUidAcrossEventsSoTheClientDoesNotCollectDuplicates() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDataProducts(List.of(dataProduct("optical", "rtsp://example.net:8554/optical_view")));

    String first = mapper.map(usv, new TwinUpdateContext()).getDetail().getVideos().get(0).getUid();
    String second = mapper.map(usv, new TwinUpdateContext()).getDetail().getVideos().get(0).getUid();

    assertEquals(first, second);
    assertNotNull(first);
  }

  @Test
  void aConfiguredIdentifierIsTheFeedsUid() {
    DroneTwin usv = twin(VehicleClass.USV);
    DataProductConfig product = dataProduct("optical", "rtsp://example.net:8554/optical_view");
    product.setIdentifier("6f1d2a3b-0000-4000-8000-000000000001");
    usv.setDataProducts(List.of(product));

    assertEquals("6f1d2a3b-0000-4000-8000-000000000001",
        mapper.map(usv, new TwinUpdateContext()).getDetail().getVideos().get(0).getUid());
  }

  @Test
  void theVideoElementIsSerialisedTheWayTheClientReadsIt() {
    DroneTwin usv = twin(VehicleClass.USV);
    usv.setDataProducts(List.of(dataProduct("optical", "rtsp://example.net:8554/optical_view")));
    TakEvent event = mapper.map(usv, new TwinUpdateContext());
    policy.apply(event, usv, null, null);

    String xml = new TakXmlSerialiser().toXml(event);

    assertTrue(xml.contains("<__video"), xml);
    assertTrue(xml.contains("url=\"rtsp://example.net:8554/optical_view\""), xml);
    assertTrue(xml.contains("<ConnectionEntry"), xml);
    assertTrue(xml.contains("protocol=\"rtsp\""), xml);
    assertTrue(xml.contains("address=\"example.net\""), xml);
    assertTrue(xml.contains("port=\"8554\""), xml);
    assertTrue(xml.contains("alias=\"optical\""), xml);
    assertTrue(xml.contains("networkTimeout="), xml);
    assertTrue(xml.contains("</__video>"), xml);
  }

  private static DataProductConfig dataProduct(String description, String uri) {
    DataProductConfig product = new DataProductConfig();
    product.setDescription(description);
    product.setUri(uri);
    return product;
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