/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.adapter.cot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinObserver;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CotIngestAdapterTest {

  @Test
  void defaultSubscriptionUsesWildcardWithNoLocal() {
    CotIngestAdapter adapter = new CotIngestAdapter(
        CotIngestAdapterFactory.DEFAULT_TOPIC,
        new TwinManager());

    SubscriptionContext context = adapter.buildSubscriptionContext();

    assertEquals("/tak/cot/inbound/#", context.getDestinationName());
    assertTrue(context.noLocalMessages());
  }

  @Test
  void archiveMessageCarriesPublishingSessionIdentity() {
    CotIngestAdapter adapter = new CotIngestAdapter(
        CotIngestAdapterFactory.DEFAULT_TOPIC,
        new TwinManager());

    Message message = adapter.buildArchiveMessage(cot("asset-1"), "cot-ingest-adapter");

    assertEquals("cot-ingest-adapter", message.getMeta().get("sessionId"));
    assertEquals("CoT", message.getMeta().get("protocol"));
    assertEquals("text/xml", message.getContentType());
    assertFalse(message.isRetain());
  }

  @Test
  void bareArchiveTopicFromAnotherSessionCanBeIngested() {
    TwinManager twinManager = new TwinManager();
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twinManager);

    adapter.handle(CotIngestAdapter.LOCAL_ARCHIVE_TOPIC, cot("asset-1"));

    assertEquals(1, twinManager.getTwinCount());
    assertEquals(1, adapter.getRoutedCount());
  }

  @Test
  void edgeLeafRoutesOnceAndCarriesEdgeIdentity() {
    TwinManager twinManager = new TwinManager();
    TwinObserver observer = mock(TwinObserver.class);
    twinManager.addObserver(observer);
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twinManager);

    adapter.handle("/tak/cot/inbound/edge-a", cot("asset-1"));

    assertEquals(1, twinManager.getTwinCount());
    assertEquals(1, adapter.getRoutedCount());

    ArgumentCaptor<TwinUpdateContext> context = ArgumentCaptor.forClass(TwinUpdateContext.class);
    verify(observer).onTwinAdded(any(), context.capture());
    assertEquals("cot-bridge-ingest", context.getValue().getUpdateSource());
    assertEquals("edge-a", context.getValue().getSourceInstanceId());
    assertEquals("/tak/cot/inbound/edge-a", context.getValue().getSourceNamespace());
  }

  @Test
  void malformedPayloadIsCountedAsDropped() {
    TwinManager twinManager = new TwinManager();
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twinManager);

    adapter.handle("/tak/cot/inbound/edge-a", "<event".getBytes(StandardCharsets.UTF_8));

    assertEquals(0, twinManager.getTwinCount());
    assertEquals(0, adapter.getRoutedCount());
    assertEquals(1, adapter.getDroppedCount());
  }


  @Test
  void archiveMessageCarriesPayloadAndProtocolMetadata() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    byte[] xml = "<event uid=\"abc\"/>".getBytes(StandardCharsets.UTF_8);

    Message message = adapter.buildArchiveMessage(xml, "session-1");

    assertArrayEquals(xml, message.getOpaqueData());
    assertEquals("text/xml", message.getContentType());
    assertEquals("CoT", message.getMeta().get("protocol"));
    assertEquals("1.0", message.getMeta().get("version"));
    assertEquals("session-1", message.getMeta().get("sessionId"));
    assertFalse(message.isRetain());
  }

  @Test
  void emptyInboundMessageIsIgnoredAndCompletionStillRuns() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    org.mockito.Mockito.when(event.getMessage()).thenReturn(message);
    org.mockito.Mockito.when(event.getCompletionTask()).thenReturn(completion);
    org.mockito.Mockito.when(message.getOpaqueData()).thenReturn(new byte[0]);

    adapter.sendMessage(event);

    assertEquals(0L, adapter.getRoutedCount());
    assertEquals(0L, adapter.getDroppedCount());
    assertEquals(-1L, adapter.getLastMessageAgeMillis());
    verify(completion).run();
  }

  @Test
  void clientConnectionIdentityIsStable() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));

    assertEquals("cot-ingest", adapter.getName());
    assertEquals("1.0", adapter.getVersion());
    assertEquals("cot-ingest-adapter", adapter.getUniqueName());
    assertEquals("internal", adapter.getProtocolName());
    assertEquals("", adapter.getAuthenticationConfig());
    assertEquals("", adapter.getRemoteIp());
    assertEquals(0L, adapter.getTimeOut());
    assertNull(adapter.getPrincipal());
    assertDoesNotThrow(adapter::sendKeepAlive);
  }

  @Test
  void sendMessage_onlyQueues_andStillRunsCompletion() {
    TwinManager twinManager = new TwinManager();
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twinManager);
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    org.mockito.Mockito.when(event.getMessage()).thenReturn(message);
    org.mockito.Mockito.when(event.getDestinationName()).thenReturn("/tak/cot/inbound/edge-a");
    org.mockito.Mockito.when(event.getCompletionTask()).thenReturn(completion);
    org.mockito.Mockito.when(message.getOpaqueData()).thenReturn(cot("asset-1"));

    adapter.sendMessage(event);

    // No worker started: nothing may have touched the twins on the delivery (calling) thread.
    assertEquals(0, twinManager.getTwinCount());
    assertEquals(1, adapter.getQueueSize());
    verify(completion).run();
  }

  @Test
  void enqueue_returnsAtOnce_whileTheTwinPipelineIsBlocked() throws Exception {
    // Same failure mode as the 2026-10-07 central stall: the delivery thread must never wait for twin updates.
    TwinManager twinManager = new TwinManager();
    CountDownLatch insideObserver = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch secondAdded = new CountDownLatch(2);
    twinManager.addObserver(new TwinObserver() {
      @Override
      public void onTwinAdded(io.mapsmessaging.state.drone.core.EntityTwin twin, TwinUpdateContext context) {
        insideObserver.countDown();
        secondAdded.countDown();
        try {
          release.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", twinManager);
    adapter.startWorker();
    try {
      adapter.enqueue("/tak/cot/inbound/edge-a", cot("asset-1"));
      assertTrue(insideObserver.await(5, TimeUnit.SECONDS), "worker should be blocked inside the twin pipeline");

      assertTimeoutPreemptively(Duration.ofSeconds(1), () -> adapter.enqueue("/tak/cot/inbound/edge-a", cot("asset-2")));
      assertEquals(1, adapter.getQueueSize());

      release.countDown();
      assertTrue(secondAdded.await(5, TimeUnit.SECONDS), "queued message should be processed once the pipeline frees up");
      assertTrue(twinManager.getTwin("asset-2").isPresent());
    } finally {
      release.countDown();
      adapter.stopWorker();
    }
  }

  @Test
  void enqueue_whenQueueIsFull_dropsTheOldestMessage() {
    CotIngestAdapter adapter = new CotIngestAdapter("/tak/cot/inbound/#", new TwinManager());

    for (int index = 0; index <= CotIngestAdapter.MAX_QUEUE_SIZE; index++) {
      adapter.enqueue("/tak/cot/inbound/edge-a", cot("asset-" + index));
    }

    assertEquals(CotIngestAdapter.MAX_QUEUE_SIZE, adapter.getQueueSize());
    assertEquals(1, adapter.getQueueFullDropCount());
  }

  private byte[] cot(String uid) {
    return ("""
        <event uid="%s" type="a-f-A-M-F-Q">
          <point lat="38.0" lon="-9.0" hae="10"/>
        </event>
        """.formatted(uid)).getBytes(StandardCharsets.UTF_8);
  }
}
