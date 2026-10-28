/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.cot;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CotProtocolFramingTest {

  @ParameterizedTest
  @MethodSource("streamPrefixes")
  void decode_finds_the_event_after_xml_declarations_or_noise(String prefix) throws Exception {
    CotStreamCodec codec = new CotXmlStreamCodec(16_384);
    byte[] event = bytes("<event uid='framed'><detail>data</detail></event>");

    List<byte[]> events = codec.decode(bytes(prefix + new String(event, StandardCharsets.UTF_8)));

    assertEquals(1, events.size());
    assertArrayEquals(event, events.getFirst());
  }

  @ParameterizedTest
  @MethodSource("streamPrefixes")
  void decode_preserves_a_split_event_after_the_prefix(String prefix) throws Exception {
    CotStreamCodec codec = new CotXmlStreamCodec(16_384);

    assertTrue(codec.decode(bytes(prefix + "<ev")).isEmpty());
    List<byte[]> events = codec.decode(bytes("ent uid='split'></event>"));

    assertEquals(1, events.size());
    assertArrayEquals(bytes("<event uid='split'></event>"), events.getFirst());
  }

  private static Stream<String> streamPrefixes() {
    return Stream.of("", "noise", " ", "\t\r\n", "<?xml?>", "<?xml?> ",
        "<?xml version='1.0'?>\n\t", "noise<?xml?> ", "<?xml?>garbage");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
