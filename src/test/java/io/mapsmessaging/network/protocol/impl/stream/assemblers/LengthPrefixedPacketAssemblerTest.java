/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.stream.assemblers;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.stream.CompleteHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LengthPrefixedPacketAssemblerTest {

  @Test
  void shouldAssembleSinglePacketDeliveredInOneChunkBigEndianFourByteLength() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        1024,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("hello", 4, LengthEndian.BIG)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldAssemblePacketWhenLengthAndPayloadArriveAcrossMultipleChunks() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        1024,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    byte[] frame = createLengthPrefixedFrame("hello", 4, LengthEndian.BIG);

    assembler.processPacket(createPacket(slice(frame, 0, 2)));
    assembler.processPacket(createPacket(slice(frame, 2, 5)));
    assembler.processPacket(createPacket(slice(frame, 5, frame.length)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldAssembleFragmentedLargePacketAcrossMultiplePackets() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        8192,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    byte[] payload = createPayload(4096, (byte) 'A');
    byte[] frame = createLengthPrefixedFrame(payload, 4, LengthEndian.BIG);

    assembler.processPacket(createPacket(slice(frame, 0, 1028)));
    assertEquals(0, completeHandler.buffers.size());

    assembler.processPacket(createPacket(slice(frame, 1028, 2052)));
    assertEquals(0, completeHandler.buffers.size());

    assembler.processPacket(createPacket(slice(frame, 2052, 3076)));
    assertEquals(0, completeHandler.buffers.size());

    assembler.processPacket(createPacket(slice(frame, 3076, frame.length)));
    assertEquals(1, completeHandler.buffers.size());
    assertEquals(4096, completeHandler.buffers.get(0).remaining());
  }

  @Test
  void shouldAssembleMultiplePacketsFromSingleInputPacket() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        1024,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    byte[] first = createLengthPrefixedFrame("one", 4, LengthEndian.BIG);
    byte[] second = createLengthPrefixedFrame("two", 4, LengthEndian.BIG);
    byte[] combined = concat(first, second);

    assembler.processPacket(createPacket(combined));

    assertEquals(2, completeHandler.buffers.size());
    assertEquals("one", toString(completeHandler.buffers.get(0)));
    assertEquals("two", toString(completeHandler.buffers.get(1)));
  }

  @Test
  void shouldHandleZeroLengthPacket() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        1024,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixOnly(0, 4, LengthEndian.BIG)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals(0, completeHandler.buffers.get(0).remaining());
  }

  @Test
  void shouldSupportOneByteLengthField() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        1,
        255,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("abc", 1, LengthEndian.BIG)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("abc", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldSupportTwoByteLengthFieldBigEndian() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        2,
        65535,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("hello", 2, LengthEndian.BIG)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldSupportTwoByteLengthFieldLittleEndian() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        2,
        65535,
        false,
        LengthEndian.LITTLE,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("hello", 2, LengthEndian.LITTLE)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldSupportThreeByteLengthFieldBigEndian() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        3,
        65535,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("hello", 3, LengthEndian.BIG)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldSupportThreeByteLengthFieldLittleEndian() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        3,
        65535,
        false,
        LengthEndian.LITTLE,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("hello", 3, LengthEndian.LITTLE)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldSupportFourByteLengthFieldLittleEndian() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        65535,
        false,
        LengthEndian.LITTLE,
        completeHandler
    );

    assembler.processPacket(createPacket(createLengthPrefixedFrame("hello", 4, LengthEndian.LITTLE)));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("hello", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldRejectPacketLargerThanConfiguredMaximum() {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        2,
        4,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    byte[] oversized = createLengthPrefixedFrame("hello", 2, LengthEndian.BIG);

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> assembler.processPacket(createPacket(oversized))
    );

    assertTrue(exception.getMessage().contains("exceeds configured maximum"));
    assertEquals(0, completeHandler.buffers.size());
  }

  @Test
  void shouldContinueAfterReset() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    LengthPrefixedPacketAssembler assembler = new LengthPrefixedPacketAssembler(
        4,
        1024,
        false,
        LengthEndian.BIG,
        completeHandler
    );

    byte[] frame = createLengthPrefixedFrame("reset", 4, LengthEndian.BIG);

    assembler.processPacket(createPacket(slice(frame, 0, 3)));
    assembler.reset();
    assembler.processPacket(createPacket(frame));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("reset", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldRejectInvalidLengthFieldSizeTooSmall() {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new LengthPrefixedPacketAssembler(0, 1024, false, LengthEndian.BIG, completeHandler)
    );

    assertTrue(exception.getMessage().contains("Length field size must be between 1 and 4 bytes"));
  }

  @Test
  void shouldRejectInvalidLengthFieldSizeTooLarge() {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new LengthPrefixedPacketAssembler(5, 1024, false, LengthEndian.BIG, completeHandler)
    );

    assertTrue(exception.getMessage().contains("Length field size must be between 1 and 4 bytes"));
  }

  private static Packet createPacket(byte[] data) {
    return new Packet(ByteBuffer.wrap(data));
  }

  private static byte[] createLengthPrefixedFrame(String value, int lengthFieldSize, LengthEndian endian) {
    return createLengthPrefixedFrame(value.getBytes(StandardCharsets.UTF_8), lengthFieldSize, endian);
  }

  private static byte[] createLengthPrefixedFrame(byte[] payload, int lengthFieldSize, LengthEndian endian) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.writeBytes(createLengthPrefixOnly(payload.length, lengthFieldSize, endian));
    outputStream.writeBytes(payload);
    return outputStream.toByteArray();
  }

  private static byte[] createLengthPrefixOnly(int length, int lengthFieldSize, LengthEndian endian) {
    byte[] bytes = new byte[lengthFieldSize];

    if (endian == LengthEndian.BIG) {
      for (int index = 0; index < lengthFieldSize; index++) {
        int shift = (lengthFieldSize - 1 - index) * 8;
        bytes[index] = (byte) ((length >> shift) & 0xff);
      }
    } else {
      for (int index = 0; index < lengthFieldSize; index++) {
        int shift = index * 8;
        bytes[index] = (byte) ((length >> shift) & 0xff);
      }
    }
    return bytes;
  }

  private static byte[] createPayload(int size, byte value) {
    byte[] payload = new byte[size];
    for (int index = 0; index < size; index++) {
      payload[index] = value;
    }
    return payload;
  }

  private static byte[] concat(byte[]... arrays) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for (byte[] array : arrays) {
      outputStream.write(array, 0, array.length);
    }
    return outputStream.toByteArray();
  }

  private static byte[] slice(byte[] data, int start, int end) {
    int length = end - start;
    byte[] result = new byte[length];
    System.arraycopy(data, start, result, 0, length);
    return result;
  }

  private static String toString(ByteBuffer byteBuffer) {
    ByteBuffer copy = byteBuffer.asReadOnlyBuffer();
    byte[] data = new byte[copy.remaining()];
    copy.get(data);
    return new String(data, StandardCharsets.UTF_8);
  }

  private static class RecordingCompleteHandler implements CompleteHandler {

    private final List<ByteBuffer> buffers = new ArrayList<>();

    @Override
    public void completePacket(ByteBuffer buffer) {
      ByteBuffer copy = ByteBuffer.allocate(buffer.remaining());
      ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
      copy.put(readOnlyBuffer);
      copy.flip();
      buffers.add(copy);
    }
  }
}