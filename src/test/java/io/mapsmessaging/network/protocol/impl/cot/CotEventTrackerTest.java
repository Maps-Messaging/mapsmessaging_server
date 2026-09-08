/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.network.protocol.impl.cot.CotEchoSuppressor.CotEventInfo;
import io.mapsmessaging.network.protocol.impl.cot.CotEventTracker.Decision;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CotEventTrackerTest {

  private static final Instant NOW = Instant.parse("2026-09-08T16:00:00Z");

  @Test
  void suppresses_direct_and_reformatted_semantic_echoes() throws Exception {
    MutableClock clock = new MutableClock(NOW);
    CotEventTracker tracker = tracker("maps-a", 4, clock);
    byte[] xml = event("target", NOW, NOW.plusSeconds(60), "one");
    CotEventInfo original = CotEchoSuppressor.inspect(xml, 64);
    byte[] reordered = new String(xml, StandardCharsets.UTF_8)
        .replace("uid=\"target\" type=\"a-f-G\"", "type=\"a-f-G\" uid=\"target\"")
        .replace("><point", ">\n  <point")
        .replace("/><detail", "/>\n  <detail")
        .getBytes(StandardCharsets.UTF_8);

    tracker.rememberOutbound(original);
    assertEquals(
        Decision.SEMANTIC_ECHO,
        tracker.evaluateInbound(CotEchoSuppressor.inspect(reordered, 64)));

    byte[] marked = CotEchoSuppressor.mark(xml, "maps-a");
    assertEquals(
        Decision.DIRECT_ECHO,
        tracker.evaluateInbound(CotEchoSuppressor.inspect(marked, 64)));
  }

  @Test
  void preserves_two_bridge_route_and_enforces_the_outbound_hop_limit() throws Exception {
    byte[] xml = event("target", NOW, NOW.plusSeconds(60), "one");
    byte[] viaA = CotEchoSuppressor.mark(xml, "maps-a");
    byte[] viaB = CotEchoSuppressor.mark(viaA, "maps-b");
    CotEventInfo routed = CotEchoSuppressor.inspect(viaB, 64);

    assertEquals(Decision.ACCEPT, tracker("maps-c", 2, new MutableClock(NOW)).evaluateInbound(routed));
    assertEquals(Decision.HOP_LIMIT, tracker("maps-c", 2, new MutableClock(NOW)).evaluateOutbound(routed));
    byte[] overLimit = CotEchoSuppressor.mark(viaB, "maps-c");
    assertEquals(
        Decision.HOP_LIMIT,
        tracker("maps-d", 2, new MutableClock(NOW))
            .evaluateInbound(CotEchoSuppressor.inspect(overLimit, 64)));
    assertEquals(Decision.DIRECT_ECHO, tracker("maps-a", 4, new MutableClock(NOW)).evaluateInbound(routed));
  }

  @Test
  void distinguishes_duplicates_newer_updates_and_out_of_order_updates() throws Exception {
    CotEventTracker tracker = tracker("maps-a", 4, new MutableClock(NOW));
    CotEventInfo first = info("target", NOW, NOW.plusSeconds(60), "one");
    CotEventInfo newer = info("target", NOW.plusSeconds(1), NOW.plusSeconds(60), "two");
    CotEventInfo older = info("target", NOW.minusSeconds(1), NOW.plusSeconds(60), "three");

    assertEquals(Decision.ACCEPT, tracker.evaluateInbound(first));
    assertEquals(Decision.DUPLICATE, tracker.evaluateInbound(first));
    assertEquals(Decision.ACCEPT, tracker.evaluateInbound(newer));
    assertEquals(Decision.OLDER_UPDATE, tracker.evaluateInbound(older));
  }

  @Test
  void applies_clock_skew_and_expires_fingerprints() throws Exception {
    MutableClock clock = new MutableClock(NOW);
    CotEventTracker tracker = tracker("maps-a", 4, clock);
    CotEventInfo withinSkew = info("target", NOW, NOW.minusSeconds(4), "one");
    CotEventInfo expired = info("expired", NOW, NOW.minusSeconds(6), "two");
    CotEventInfo echoed = info("echo", NOW, NOW.plusSeconds(60), "three");

    assertEquals(Decision.ACCEPT, tracker.evaluateInbound(withinSkew));
    assertEquals(Decision.EXPIRED, tracker.evaluateInbound(expired));
    tracker.rememberOutbound(echoed);
    assertEquals(Decision.SEMANTIC_ECHO, tracker.evaluateInbound(echoed));
    clock.advance(Duration.ofSeconds(121));
    assertEquals(Decision.EXPIRED, tracker.evaluateInbound(echoed));
  }

  @Test
  void rejects_events_that_have_not_started_beyond_the_clock_skew() throws Exception {
    CotEventTracker tracker = tracker("maps-a", 4, new MutableClock(NOW));
    CotEventInfo withinSkew = info("near", NOW.plusSeconds(4), NOW.plusSeconds(60), "one");
    CotEventInfo future = info("future", NOW.plusSeconds(6), NOW.plusSeconds(60), "two");

    assertEquals(Decision.ACCEPT, tracker.evaluateInbound(withinSkew));
    assertEquals(Decision.NOT_STARTED, tracker.evaluateInbound(future));
    assertEquals(Decision.NOT_STARTED, tracker.evaluateOutbound(future));
  }

  private CotEventTracker tracker(String origin, int hops, Clock clock) {
    return new CotEventTracker(
        origin,
        hops,
        16,
        Duration.ofSeconds(120),
        Duration.ofSeconds(5),
        16,
        clock);
  }

  private CotEventInfo info(String uid, Instant time, Instant stale, String value) throws Exception {
    return CotEchoSuppressor.inspect(event(uid, time, stale, value), 64);
  }

  private byte[] event(String uid, Instant time, Instant stale, String value) {
    String xml = "<event version=\"2.0\" uid=\"" + uid
        + "\" type=\"a-f-G\" how=\"m-g\" time=\"" + time
        + "\" start=\"" + time + "\" stale=\"" + stale
        + "\"><point lat=\"1\" lon=\"2\" hae=\"3\" ce=\"4\" le=\"5\"/>"
        + "<detail><remarks>" + value + "</remarks></detail></event>";
    return xml.getBytes(StandardCharsets.UTF_8);
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
