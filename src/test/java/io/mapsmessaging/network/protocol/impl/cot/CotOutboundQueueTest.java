/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.cot.CotEchoSuppressor.CotEventInfo;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CotOutboundQueueTest {

  private static final Instant NOW = Instant.parse("2026-09-08T16:00:00Z");

  @Test
  void concurrent_publishers_produce_complete_non_interleaved_frames() throws Exception {
    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    List<ServerPacket> frames = new ArrayList<>();
    AtomicInteger completed = new AtomicInteger();
    MutableClock clock = new MutableClock(NOW);
    CotOutboundQueue queue = queue(2048, 17, frames::add, clock);
    ExecutorService publishers = Executors.newFixedThreadPool(8);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();
    for (int thread = 0; thread < 8; thread++) {
      int source = thread;
      futures.add(publishers.submit(() -> {
        start.await();
        for (int index = 0; index < 100; index++) {
          byte[] xml = event("u-" + source + '-' + index, NOW, NOW.plusSeconds(60), "v");
          queue.offer(xml, info(xml), completed::incrementAndGet);
        }
        return null;
      }));
    }
    start.countDown();
    for (Future<?> future : futures) {
      future.get();
    }
    publishers.shutdownNow();
    while (!frames.isEmpty()) {
      writeAndComplete(frames.removeFirst(), wire);
    }

    String stream = wire.toString(StandardCharsets.UTF_8);
    assertEquals(800, completed.get());
    assertEquals(800, stream.split("</event>", -1).length - 1);
    assertEquals(0, queue.size());
  }

  @Test
  void coalesces_state_and_discards_it_when_stale_before_dispatch() throws Exception {
    MutableClock clock = new MutableClock(NOW);
    List<ServerPacket> frames = new ArrayList<>();
    AtomicInteger firstCompletion = new AtomicInteger();
    AtomicInteger oldStateCompletion = new AtomicInteger();
    AtomicInteger newStateCompletion = new AtomicInteger();
    CotOutboundQueue queue = queue(3, 4096, frames::add, clock);
    byte[] chat = event("chat", NOW, NOW.plusSeconds(60), "b-t-f");
    byte[] oldState = event("target", NOW, NOW.plusSeconds(2), "a-f-G");
    byte[] newState = event("target", NOW.plusSeconds(1), NOW.plusSeconds(2), "a-f-G");

    queue.offer(chat, info(chat), firstCompletion::incrementAndGet);
    queue.offer(oldState, info(oldState), oldStateCompletion::incrementAndGet);
    assertEquals(CotOutboundQueue.OfferResult.COALESCED,
        queue.offer(newState, info(newState), newStateCompletion::incrementAndGet));
    assertEquals(1, oldStateCompletion.get());

    clock.advance(Duration.ofSeconds(3));
    frames.removeFirst().complete();
    assertEquals(1, firstCompletion.get());
    assertEquals(1, newStateCompletion.get());
    assertEquals(0, queue.size());
  }

  @Test
  void close_completes_an_active_write_and_all_queued_events_once() throws Exception {
    MutableClock clock = new MutableClock(NOW);
    List<ServerPacket> frames = new ArrayList<>();
    AtomicInteger completions = new AtomicInteger();
    CotOutboundQueue queue = queue(4, 4096, frames::add, clock);
    byte[] first = event("one", NOW, NOW.plusSeconds(60), "b-t-f");
    byte[] second = event("two", NOW, NOW.plusSeconds(60), "b-t-f");

    queue.offer(first, info(first), completions::incrementAndGet);
    queue.offer(second, info(second), completions::incrementAndGet);
    queue.close();
    frames.getFirst().complete();

    assertEquals(2, completions.get());
    assertEquals(0, queue.size());
  }

  @Test
  void reports_a_stalled_in_flight_write_without_sleeping() throws Exception {
    MutableClock clock = new MutableClock(NOW);
    CotOutboundQueue queue = queue(2, 4096, ignored -> { }, clock);
    byte[] event = event("one", NOW, NOW.plusSeconds(60), "b-t-f");

    queue.offer(event, info(event), null);
    assertTrue(!queue.isWriteTimedOut(NOW.plusSeconds(29), Duration.ofSeconds(30)));
    assertTrue(queue.isWriteTimedOut(NOW.plusSeconds(30), Duration.ofSeconds(30)));
  }

  private CotOutboundQueue queue(
      int capacity,
      int chunkSize,
      java.util.function.Consumer<ServerPacket> sink,
      Clock clock) {
    return new CotOutboundQueue(
        capacity,
        chunkSize,
        sink,
        event -> event.stale().isAfter(clock.instant()),
        ignored -> { },
        ignored -> { },
        ignored -> { },
        clock);
  }

  private void writeAndComplete(ServerPacket frame, ByteArrayOutputStream wire) {
    synchronized (wire) {
      Packet packet = new Packet(4096, false);
      frame.packFrame(packet);
      packet.flip();
      byte[] bytes = new byte[packet.available()];
      packet.get(bytes);
      wire.writeBytes(bytes);
      frame.complete();
    }
  }

  private CotEventInfo info(byte[] xml) throws Exception {
    return CotEchoSuppressor.inspect(xml, 64);
  }

  private byte[] event(String uid, Instant time, Instant stale, String type) {
    return ("<event version=\"2.0\" uid=\"" + uid + "\" type=\"" + type
        + "\" how=\"m-g\" time=\"" + time + "\" start=\"" + time
        + "\" stale=\"" + stale + "\"><point lat=\"1\" lon=\"2\" hae=\"3\""
        + " ce=\"4\" le=\"5\"/><detail/></event>").getBytes(StandardCharsets.UTF_8);
  }

  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
