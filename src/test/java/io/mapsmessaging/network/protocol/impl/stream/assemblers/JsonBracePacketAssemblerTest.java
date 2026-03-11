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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonBracePacketAssemblerTest {

  @Test
  void shouldAssembleSingleJsonObjectDeliveredInOneChunk() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("{\"name\":\"matthew\"}"));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("{\"name\":\"matthew\"}", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldAssembleJsonObjectAcrossMultipleChunks() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("{\"name\""));
    assembler.processPacket(createPacket(":\"matt"));
    assembler.processPacket(createPacket("hew\"}"));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("{\"name\":\"matthew\"}", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldHandleNestedJsonObjects() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("{\"outer\":{\"inner\":1}}"));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("{\"outer\":{\"inner\":1}}", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldIgnoreDataBeforeFirstOpeningBrace() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("noise noise {\"value\":1}"));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("{\"value\":1}", toString(completeHandler.buffers.get(0)));
  }

  @Test
  void shouldExtractMultipleJsonObjectsFromSinglePacket() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("{\"a\":1}{\"b\":2}"));

    assertEquals(2, completeHandler.buffers.size());
    assertEquals("{\"a\":1}", toString(completeHandler.buffers.get(0)));
    assertEquals("{\"b\":2}", toString(completeHandler.buffers.get(1)));
  }

  @Test
  void shouldContinueWithSecondObjectAfterFirstCompletedAcrossBoundaries() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("{\"a\":1}{\"b\""));
    assembler.processPacket(createPacket(":2}"));

    assertEquals(2, completeHandler.buffers.size());
    assertEquals("{\"a\":1}", toString(completeHandler.buffers.get(0)));
    assertEquals("{\"b\":2}", toString(completeHandler.buffers.get(1)));
  }

  @Test
  void shouldRejectJsonObjectThatExceedsConfiguredMaximum() {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(10, false, completeHandler);

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> assembler.processPacket(createPacket("{\"abcdef\":1}"))
    );

    assertTrue(exception.getMessage().contains("exceeds configured maximum"));
    assertEquals(0, completeHandler.buffers.size());
    assertEquals(0, assembler.getBraceDepth());
  }

  @Test
  void shouldResetAndProcessNextObject() throws IOException {
    RecordingCompleteHandler completeHandler = new RecordingCompleteHandler();
    JsonBracePacketAssembler assembler = new JsonBracePacketAssembler(1024, false, completeHandler);

    assembler.processPacket(createPacket("{\"value\""));
    assembler.reset();
    assembler.processPacket(createPacket("{\"value\":2}"));

    assertEquals(1, completeHandler.buffers.size());
    assertEquals("{\"value\":2}", toString(completeHandler.buffers.get(0)));
  }

  private static Packet createPacket(String data) {
    return new Packet(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
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