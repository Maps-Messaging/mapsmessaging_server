/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkFrameExtractorTest {

  @Test
  void extractsBackToBackV1FramesUsingPayloadPlusEightBytes() {
    byte[] first = {(byte) 0xFE, 2, 7, 11, 3, 42, 90, 91, 12, 13};
    byte[] second = {(byte) 0xFE, 1, 8, 12, 4, 43, 92, 14, 15};
    byte[] input = concat(first, second);

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(input);

    assertEquals(2, frames.size());
    assertArrayEquals(first, frames.get(0));
    assertArrayEquals(second, frames.get(1));
    assertEquals(11, MavlinkFrameExtractor.getSystemId(first));
  }

  @Test
  void ignoresNoiseBeforeValidV1Frame() {
    byte[] frame = {(byte) 0xFE, 0, 1, 21, 2, 0, 10, 11};
    byte[] input = concat(new byte[]{1, 2, 3, 4}, frame);

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(input);

    assertEquals(1, frames.size());
    assertArrayEquals(frame, frames.getFirst());
  }

  @Test
  void truncatedV1FrameIsIgnored() {
    byte[] input = {(byte) 0xFE, 4, 1, 2, 3};

    assertTrue(MavlinkFrameExtractor.extractMavlinkFrames(input).isEmpty());
  }

  @Test
  void loneV1MagicByteIsIgnored() {
    assertTrue(MavlinkFrameExtractor.extractMavlinkFrames(
        new byte[]{(byte) 0xFE}).isEmpty());
  }

  @Test
  void extractsUnsignedV2Frame() {
    byte[] frame = new byte[12];
    frame[0] = (byte) 0xFD;
    frame[1] = 0;
    frame[2] = 0;
    frame[5] = 33;

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(frame);

    assertEquals(1, frames.size());
    assertArrayEquals(frame, frames.getFirst());
    assertEquals(33, MavlinkFrameExtractor.getSystemId(frame));
  }

  @Test
  void extractsSignedV2FrameIncludingSignature() {
    byte[] frame = new byte[25];
    frame[0] = (byte) 0xFD;
    frame[1] = 0;
    frame[2] = 1;
    frame[5] = 44;
    frame[24] = 99;

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(frame);

    assertEquals(1, frames.size());
    assertArrayEquals(frame, frames.getFirst());
  }

  @Test
  void truncatedSignedV2FrameIsIgnored() {
    byte[] frame = new byte[24];
    frame[0] = (byte) 0xFD;
    frame[1] = 0;
    frame[2] = 1;

    assertTrue(MavlinkFrameExtractor.extractMavlinkFrames(frame).isEmpty());
  }

  @Test
  void truncatedV2HeaderIsIgnored() {
    assertTrue(MavlinkFrameExtractor.extractMavlinkFrames(
        new byte[]{(byte) 0xFD, 0}).isEmpty());
  }

  @Test
  void completeFrameBeforeTruncatedTrailingFrameIsRetained() {
    byte[] complete = {(byte) 0xFE, 0, 1, 21, 2, 0, 10, 11};
    byte[] trailing = {(byte) 0xFD, 4, 0};
    byte[] input = concat(complete, trailing);

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(input);

    assertEquals(1, frames.size());
    assertArrayEquals(complete, frames.getFirst());
  }

  @Test
  void emptyInputProducesNoFrames() {
    assertTrue(MavlinkFrameExtractor.extractMavlinkFrames(new byte[0]).isEmpty());
  }

  @Test
  void unknownMagicIsRejectedBySystemIdLookup() {
    assertThrows(IllegalArgumentException.class,
        () -> MavlinkFrameExtractor.getSystemId(new byte[]{0x01, 0, 0, 0, 0, 0}));
  }

  private byte[] concat(byte[] first, byte[] second) {
    byte[] input = new byte[first.length + second.length];
    System.arraycopy(first, 0, input, 0, first.length);
    System.arraycopy(second, 0, input, first.length, second.length);
    return input;
  }
}
