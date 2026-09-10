/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CotProtocolDetectionTest {

  private final CotProtocolDetection detection = new CotProtocolDetection();

  @Test
  void detectsEventAfterXmlDeclaration() throws Exception {
    assertTrue(detection.detected(packet("<?xml version=\"1.0\"?><event uid=\"x\">")));
  }

  @Test
  void waitsForFragmentedHeader() {
    assertThrows(EndOfBufferException.class, () -> detection.detected(packet("<eve")));
  }

  @Test
  void rejectsNonCotHeaderAtDetectionLimit() throws Exception {
    assertFalse(detection.detected(packet("x".repeat(detection.getHeaderSize()))));
  }

  @Test
  void rejectsElementNamesThatOnlyStartWithEvent() throws Exception {
    String input = "<eventual>not CoT</eventual>";
    assertFalse(detection.detected(packet(input + "x".repeat(detection.getHeaderSize()))));
  }

  @Test
  void waitsForTheEventElementNameBoundary() {
    assertThrows(EndOfBufferException.class, () -> detection.detected(packet("<event")));
  }

  private Packet packet(String value) {
    return new Packet(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
  }
}
