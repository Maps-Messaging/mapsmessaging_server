/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.cot.CotStreamDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CotStreamIntegrationTest {

  @Test
  void decodes_every_fragmentation_boundary_including_utf8_content() throws Exception {
    byte[] event = event("one", "café 航空");
    for (int boundary = 1; boundary < event.length; boundary++) {
      CotStreamDecoder decoder = new CotStreamDecoder(4096);
      List<byte[]> decoded = new ArrayList<>();
      decoded.addAll(decoder.accept(Arrays.copyOfRange(event, 0, boundary)));
      decoded.addAll(decoder.accept(Arrays.copyOfRange(event, boundary, event.length)));
      assertEquals(1, decoded.size(), "boundary " + boundary);
      assertArrayEquals(event, decoded.getFirst(), "boundary " + boundary);
    }
  }

  @Test
  void decodes_multiple_events_and_retains_a_partial_following_event() throws Exception {
    byte[] first = event("one", "first");
    byte[] second = event("two", "second");
    byte[] third = event("three", "third");
    byte[] prefix = Arrays.copyOfRange(third, 0, third.length / 2);
    CotStreamDecoder decoder = new CotStreamDecoder(4096);

    List<byte[]> decoded = decoder.accept(join(
        " \r\n<?xml version='1.0'?>\n".getBytes(StandardCharsets.UTF_8),
        first,
        "\n\t".getBytes(StandardCharsets.UTF_8),
        second,
        prefix));
    assertEquals(List.of(text(first), text(second)), decoded.stream().map(this::text).toList());
    assertEquals(1, decoder.accept(Arrays.copyOfRange(third, prefix.length, third.length)).size());
  }

  @Test
  void a_partial_disconnect_does_not_contaminate_a_new_decoder_generation() throws Exception {
    byte[] event = event("one", "value");
    CotStreamDecoder oldConnection = new CotStreamDecoder(4096);
    assertEquals(0, oldConnection.accept(Arrays.copyOf(event, event.length / 2)).size());

    CotStreamDecoder newConnection = new CotStreamDecoder(4096);
    assertEquals(1, newConnection.accept(event).size());
  }

  @Test
  void malformed_complete_event_can_be_rejected_without_losing_the_next_frame() throws Exception {
    byte[] malformed = "<event uid=\"bad\"><detail/></event>".getBytes(StandardCharsets.UTF_8);
    byte[] valid = event("good", "value");
    CotStreamDecoder decoder = new CotStreamDecoder(4096);
    List<byte[]> frames = decoder.accept(join(malformed, valid));

    assertEquals(2, frames.size());
    assertThrows(IOException.class, () -> CotEchoSuppressor.inspect(frames.get(0), 64));
    assertEquals("good", CotEchoSuppressor.inspect(frames.get(1), 64).uid());
  }

  @Test
  void enforces_the_configured_maximum_event_size_and_recovers() throws Exception {
    CotStreamDecoder decoder = new CotStreamDecoder(256);
    assertThrows(IOException.class,
        () -> decoder.accept(("<event>" + "x".repeat(260)).getBytes(StandardCharsets.UTF_8)));
    assertEquals(1, decoder.accept("<event uid=\"good\"></event>".getBytes(StandardCharsets.UTF_8)).size());
  }

  private byte[] event(String uid, String detail) {
    return ("<event version=\"2.0\" uid=\"" + uid + "\" type=\"a-f-G\" how=\"m-g\""
        + " time=\"2026-09-08T16:00:00Z\" start=\"2026-09-08T16:00:00Z\""
        + " stale=\"2026-09-08T16:02:00Z\"><point lat=\"1\" lon=\"2\" hae=\"3\""
        + " ce=\"4\" le=\"5\"/><detail><remarks>" + detail
        + "</remarks></detail></event>").getBytes(StandardCharsets.UTF_8);
  }

  private byte[] join(byte[]... values) {
    int length = Arrays.stream(values).mapToInt(value -> value.length).sum();
    byte[] joined = new byte[length];
    int offset = 0;
    for (byte[] value : values) {
      System.arraycopy(value, 0, joined, offset, value.length);
      offset += value.length;
    }
    return joined;
  }

  private String text(byte[] value) {
    return new String(value, StandardCharsets.UTF_8);
  }
}
