/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.adapter.cot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CotIngestAdapterTest {

  @Test
  void defaultSubscriptionTargetsExactlyOneEdgeLeaf() {
    assertEquals("/tak/cot/inbound/+", CotIngestAdapterFactory.DEFAULT_TOPIC);
  }

  @Test
  void localArchiveTopicIsNeverRoutedAgain() {
    TwinManager twinManager = new TwinManager();
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twinManager);

    adapter.handle(CotIngestAdapter.LOCAL_ARCHIVE_TOPIC, cot("asset-1"));

    assertEquals(0, twinManager.getTwinCount());
    assertEquals(0, adapter.getRoutedCount());
    assertEquals(0, adapter.getDroppedCount());
  }

  @Test
  void edgeLeafRoutesOnceAndCarriesEdgeIdentity() {
    TwinManager twinManager = new TwinManager();
    TwinObserver observer = mock(TwinObserver.class);
    twinManager.addObserver(observer);
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/+", twinManager);

    adapter.handle("/tak/cot/inbound/edge-a", cot("asset-1"));

    assertEquals(1, twinManager.getTwinCount());
    assertEquals(1, adapter.getRoutedCount());

    ArgumentCaptor<TwinUpdateContext> context = ArgumentCaptor.forClass(TwinUpdateContext.class);
    verify(observer).onTwinAdded(any(), context.capture());
    assertEquals("cot-bridge-ingest:edge-a", context.getValue().getUpdateSource());
  }

  @Test
  void malformedPayloadIsCountedAsDropped() {
    TwinManager twinManager = new TwinManager();
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/+", twinManager);

    adapter.handle("/tak/cot/inbound/edge-a", "<event".getBytes(StandardCharsets.UTF_8));

    assertEquals(0, twinManager.getTwinCount());
    assertEquals(0, adapter.getRoutedCount());
    assertEquals(1, adapter.getDroppedCount());
  }

  private byte[] cot(String uid) {
    return ("""
        <event uid="%s" type="a-f-A-M-F-Q">
          <point lat="38.0" lon="-9.0" hae="10"/>
        </event>
        """.formatted(uid)).getBytes(StandardCharsets.UTF_8);
  }
}
