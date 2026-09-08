/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.network.protocol.impl.cot.CotEchoSuppressor.CotEventInfo;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CotInboundQueueTest {

  private static final Instant NOW = Instant.parse("2026-09-08T16:00:00Z");

  @Test
  void coalesces_state_for_a_slow_consumer_without_blocking_the_caller() throws Exception {
    ManualExecutor executor = new ManualExecutor();
    List<String> processed = new ArrayList<>();
    AtomicInteger dropped = new AtomicInteger();
    CotInboundQueue queue = queue(2, executor, processed, dropped);
    byte[] first = event("target", NOW, "a-f-G", "one");
    byte[] newer = event("target", NOW.plusSeconds(1), "a-f-G", "two");

    assertEquals(CotInboundQueue.OfferResult.ENQUEUED, queue.offer(first, info(first)));
    assertEquals(CotInboundQueue.OfferResult.COALESCED, queue.offer(newer, info(newer)));
    assertEquals(1, dropped.get());
    assertEquals(1, executor.size());

    executor.runNext();
    assertEquals(List.of("target:two"), processed);
  }

  @Test
  void overflow_evicts_low_value_state_to_preserve_chat() throws Exception {
    ManualExecutor executor = new ManualExecutor();
    List<String> processed = new ArrayList<>();
    AtomicInteger dropped = new AtomicInteger();
    CotInboundQueue queue = queue(2, executor, processed, dropped);
    byte[] stateOne = event("one", NOW, "a-f-G", "state-one");
    byte[] stateTwo = event("two", NOW, "a-f-G", "state-two");
    byte[] chat = event("chat", NOW, "b-t-f", "chat");

    queue.offer(stateOne, info(stateOne));
    queue.offer(stateTwo, info(stateTwo));
    queue.offer(chat, info(chat));
    executor.runNext();

    assertEquals(1, dropped.get());
    assertEquals(List.of("two:state-two", "chat:chat"), processed);
  }

  @Test
  void close_discards_queued_work_before_the_consumer_runs() throws Exception {
    ManualExecutor executor = new ManualExecutor();
    List<String> processed = new ArrayList<>();
    CotInboundQueue queue = queue(1, executor, processed, new AtomicInteger());
    byte[] xml = event("target", NOW, "a-f-G", "one");

    queue.offer(xml, info(xml));
    queue.close();
    executor.runNext();

    assertEquals(List.of(), processed);
    assertEquals(0, queue.size());
  }

  private CotInboundQueue queue(
      int capacity,
      Executor executor,
      List<String> processed,
      AtomicInteger dropped) {
    return new CotInboundQueue(
        capacity,
        executor,
        (xml, info) -> processed.add(info.uid() + ':' + detail(xml)),
        (xml, info) -> dropped.incrementAndGet(),
        (info, exception) -> { });
  }

  private String detail(byte[] xml) {
    String value = new String(xml, StandardCharsets.UTF_8);
    int start = value.indexOf("<remarks>") + "<remarks>".length();
    return value.substring(start, value.indexOf("</remarks>"));
  }

  private CotEventInfo info(byte[] xml) throws Exception {
    return CotEchoSuppressor.inspect(xml, 64);
  }

  private byte[] event(String uid, Instant time, String type, String detail) {
    return ("<event version=\"2.0\" uid=\"" + uid + "\" type=\"" + type
        + "\" how=\"m-g\" time=\"" + time + "\" start=\"" + time
        + "\" stale=\"" + time.plusSeconds(60) + "\"><point lat=\"1\" lon=\"2\""
        + " hae=\"3\" ce=\"4\" le=\"5\"/><detail><remarks>" + detail
        + "</remarks></detail></event>").getBytes(StandardCharsets.UTF_8);
  }

  private static final class ManualExecutor implements Executor {

    private final Deque<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable command) {
      tasks.addLast(command);
    }

    int size() {
      return tasks.size();
    }

    void runNext() {
      tasks.removeFirst().run();
    }
  }
}
