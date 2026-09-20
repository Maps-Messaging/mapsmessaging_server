/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.adapter.twin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.DetectionEvent;
import io.mapsmessaging.state.drone.model.DetectionEventType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A node that owns a vehicle publishes its twin, and the contacts the vehicle detects, to
 * {@code /state/twins/<id>/contacts}. Those documents reach an aggregator verbatim, and this is
 * what turns one back into a detection so the aggregator -- the node that actually holds the TAK
 * connection -- can publish it as CoT.
 *
 * <p>The document below is a real one, taken off an aggregator on 2026-09-20 and trimmed: the
 * contact, the asset uuid that identifies which twin detected it, and the asset's data products.
 */
class TwinContactMapperTest {

  private static final String ASSET_UUID = "287e6570-34e8-5392-9dc9-1b0d384865ec";
  private static final String CONTACT_ID = "b5052695-27b9-34f7-a674-8d25e414a43d";

  private TwinManager twinManager;
  private TwinContactMapper mapper;
  private List<DetectionEvent> raised;
  private List<DroneTwin> sources;

  @BeforeEach
  void setUp() {
    twinManager = new TwinManager();
    mapper = new TwinContactMapper(twinManager);
    raised = new ArrayList<>();
    sources = new ArrayList<>();
    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onDetectionEvent(DroneTwin source, DetectionEvent event, TwinUpdateContext context) {
        sources.add(source);
        raised.add(event);
      }
    });
  }

  private DroneTwin registerAggregatorTwin(String twinId) {
    // an aggregator keys its own twins by the asset uuid, built from the inbound CATL
    DroneTwin twin = new DroneTwin(twinId, UUID.fromString(ASSET_UUID));
    twinManager.registerTwin(twin, new TwinUpdateContext());
    return twin;
  }

  private static String document(String body) {
    return "{\"twinId\":\"USV-002\",\"uuid\":\"" + ASSET_UUID + "\"," + body + "}";
  }

  private static final String CONTACT =
      "\"contact\":{\"id\":\"" + CONTACT_ID + "\",\"description\":\"DRONE_STAT\","
          + "\"position\":{\"latitude\":38.4183989,\"longitude\":-9.0786336,"
          + "\"altitudeMslMeters\":4.34,\"altitudeRelativeMeters\":-0.049},"
          + "\"ttlMillis\":60000,\"createdTimeMs\":1789906655243,\"updatedTimeMs\":1789906728537}";

  private static final String DATA_PRODUCTS =
      "\"dataProducts\":["
          + "{\"description\":\"optical\",\"uri\":\"rtsp://viewer:secret@example.net:8554/optical_view\"},"
          + "{\"description\":\"thermal\",\"uri\":\"rtsp://viewer:secret@example.net:8554/thermal_view\"},"
          + "{\"description\":\"optical viewer\",\"uri\":\"https://example.net/optical_view/\"},"
          + "{\"description\":\"thermal viewer\",\"uri\":\"https://example.net/thermal_view/\"}]";

  @Test
  void aRelayedContactBecomesADetectionOnTheTwinThatSawIt() {
    DroneTwin twin = registerAggregatorTwin(ASSET_UUID);

    assertTrue(mapper.ingest(document(CONTACT + "," + DATA_PRODUCTS), new TwinUpdateContext()));

    assertEquals(1, raised.size());
    DetectionEvent event = raised.get(0);
    assertEquals(UUID.fromString(CONTACT_ID), event.getContactId());
    assertEquals("DRONE_STAT", event.getName());
    assertEquals(38.4183989d, event.getPosition().getLatitude());
    assertEquals(-9.0786336d, event.getPosition().getLongitude());
    assertEquals(4.34d, event.getPosition().getAltitudeMslMeters());
    assertEquals(60000L, event.getTtlMillis());
    assertEquals(1789906728537L, event.getTimestamp().toEpochMilli());
    // the detection belongs to the twin the aggregator already shows, so the CoT parent link
    // points at that marker instead of inventing a second vehicle
    assertEquals(twin, sources.get(0));
  }

  @Test
  void onlyThePlayableFeedsRideAlongWithTheDetection() {
    registerAggregatorTwin(ASSET_UUID);
    mapper.ingest(document(CONTACT + "," + DATA_PRODUCTS), new TwinUpdateContext());

    Object urls = raised.get(0).getAttributes().get("tak.videoUrls");
    assertNotNull(urls, "the detection carries no video urls");
    assertEquals(List.of("rtsp://viewer:secret@example.net:8554/optical_view",
        "rtsp://viewer:secret@example.net:8554/thermal_view"), urls);
  }

  @Test
  void aFirstSightingIsDetectedAndALaterOneIsAnUpdate() {
    registerAggregatorTwin(ASSET_UUID);
    String first = CONTACT.replace("\"updatedTimeMs\":1789906728537", "\"updatedTimeMs\":1789906655243");

    mapper.ingest(document(first), new TwinUpdateContext());
    mapper.ingest(document(CONTACT), new TwinUpdateContext());

    assertEquals(DetectionEventType.DETECTED, raised.get(0).getEventType());
    assertEquals(DetectionEventType.UPDATED, raised.get(1).getEventType());
  }

  @Test
  void aContactForAnUnknownAssetIsIgnored() {
    // no twin registered: raising a detection here would strand it with no parent marker
    assertFalse(mapper.ingest(document(CONTACT), new TwinUpdateContext()));
    assertTrue(raised.isEmpty());
  }

  @Test
  void theTwinIsFoundByUuidEvenWhenItIsKeyedByName() {
    DroneTwin twin = registerAggregatorTwin("USV-002");   // keyed by name, uuid still matches

    assertTrue(mapper.ingest(document(CONTACT), new TwinUpdateContext()));
    assertEquals(twin, sources.get(0));
  }

  @Test
  void aDocumentWithoutAContactRaisesNothing() {
    registerAggregatorTwin(ASSET_UUID);
    assertFalse(mapper.ingest(document(DATA_PRODUCTS), new TwinUpdateContext()));
    assertFalse(mapper.ingest("{\"uuid\":\"" + ASSET_UUID + "\",\"contact\":null}", new TwinUpdateContext()));
    assertTrue(raised.isEmpty());
  }

  @Test
  void aContactThatCannotBeDrawnIsIgnored() {
    registerAggregatorTwin(ASSET_UUID);
    String noPosition = "\"contact\":{\"id\":\"" + CONTACT_ID + "\",\"ttlMillis\":60000}";
    String noTtl = "\"contact\":{\"id\":\"" + CONTACT_ID + "\",\"position\":{\"latitude\":1.0,\"longitude\":2.0}}";

    assertFalse(mapper.ingest(document(noPosition), new TwinUpdateContext()));
    assertFalse(mapper.ingest(document(noTtl), new TwinUpdateContext()));
    assertTrue(raised.isEmpty());
  }

  @Test
  void rubbishIsSwallowedRatherThanKillingTheSubscription() {
    registerAggregatorTwin(ASSET_UUID);
    assertFalse(mapper.ingest("not json at all", new TwinUpdateContext()));
    assertFalse(mapper.ingest("", new TwinUpdateContext()));
    assertFalse(mapper.ingest(null, new TwinUpdateContext()));
    assertTrue(raised.isEmpty());
  }
}
