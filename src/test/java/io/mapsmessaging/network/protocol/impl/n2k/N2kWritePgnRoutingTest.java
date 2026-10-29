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

package io.mapsmessaging.network.protocol.impl.n2k;

import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.n2k.framing.FramePacker;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.formatters.impl.CanbusFormatter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** MSG-422: exercise real routing/packing with only the hardware endpoint replaced. */
class N2kWritePgnRoutingTest {

  @ParameterizedTest
  @CsvSource({"60928,8,0x18EE4D2A", "59904,3,0x18EA4D2A", "126993,8,0x19F0112A"})
  void writePgn_single_frame_pgn_preserves_can_identifier_and_payload(int pgn, int length, int identifier)
      throws Exception {
    Fixture fixture = fixture(defaultDefinitions());
    byte[] payload = payload(length);
    byte[] original = payload.clone();

    fixture.protocol.writePgn(pgn, 77, payload);

    assertEquals(1, fixture.single.size());
    assertTrue(fixture.groups.isEmpty());
    CanFrame frame = fixture.single.get(0);
    assertEquals(identifier, frame.canIdentifier());
    assertTrue(frame.extendedFrame());
    assertEquals(length, frame.dataLengthCode());
    assertArrayEquals(original, frame.data());
    assertArrayEquals(original, payload);
  }

  @ParameterizedTest
  @CsvSource({"126996,14,0x19F0142A,3", "126998,3,0x19F0162A,1"})
  void writePgn_fast_packet_is_one_group_with_unchanged_identifier_and_payload(
      int pgn, int length, int identifier, int expectedFrames) throws Exception {
    Fixture fixture = fixture(defaultDefinitions());
    byte[] payload = payload(length);
    byte[] original = payload.clone();

    fixture.protocol.writePgn(pgn, 77, payload);

    assertTrue(fixture.single.isEmpty());
    assertEquals(1, fixture.groups.size());
    List<CanFrame> frames = fixture.groups.get(0);
    assertEquals(expectedFrames, frames.size());
    byte[] reconstructed = new byte[length];
    int offset = 0;
    int sequence = frames.get(0).data()[0] & 0xE0;
    for (int index = 0; index < frames.size(); index++) {
      CanFrame frame = frames.get(index);
      assertEquals(identifier, frame.canIdentifier());
      assertTrue(frame.extendedFrame());
      assertEquals(8, frame.dataLengthCode());
      assertEquals(sequence | index, frame.data()[0] & 0xFF);
      int start = index == 0 ? 2 : 1;
      if (index == 0) {
        assertEquals(length, frame.data()[1] & 0xFF);
      }
      int count = Math.min(8 - start, length - offset);
      System.arraycopy(frame.data(), start, reconstructed, offset, count);
      offset += count;
    }
    assertArrayEquals(original, reconstructed);
    assertArrayEquals(original, payload);
  }

  @Test
  void writePgn_uses_configured_registry_transport_for_same_pgn_and_payload() throws Exception {
    Fixture single = fixture(definition(130000, "Single", 3));
    Fixture fast = fixture(definition(130000, "Fast", 3));
    byte[] data = {1, 2, 3};

    single.protocol.writePgn(130000, 77, data);
    fast.protocol.writePgn(130000, 77, data);

    assertEquals(1, single.single.size());
    assertTrue(single.groups.isEmpty());
    assertArrayEquals(data, single.single.get(0).data());
    assertEquals(0x19FBD02A, single.single.get(0).canIdentifier());
    assertTrue(fast.single.isEmpty());
    assertEquals(1, fast.groups.size());
    assertEquals(3, fast.groups.get(0).get(0).data()[1] & 0xFF);
  }

  @Test
  void writePgn_unknown_pgn_up_to_eight_bytes_uses_single_frame_fallback() throws Exception {
    for (int length : new int[]{0, 1, 8}) {
      Fixture fixture = fixture(defaultDefinitions());
      assertNull(fixture.formatter.getParser().getRegistry().getRequiredMessage(130001));
      byte[] data = payload(length);

      fixture.protocol.writePgn(130001, 77, data);

      assertEquals(1, fixture.single.size(), "length " + length);
      assertTrue(fixture.groups.isEmpty());
      assertEquals(0x19FBD12A, fixture.single.get(0).canIdentifier());
      assertArrayEquals(data, fixture.single.get(0).data());
    }
  }

  @Test
  void writePgn_unknown_pgn_above_eight_bytes_fails_without_writing() throws Exception {
    Fixture fixture = fixture(defaultDefinitions());
    assertRejectedWithoutWrite(fixture, 130001, payload(9));
  }

  @Test
  void writePgn_unsupported_transport_fails_without_writing() throws Exception {
    Fixture fixture = fixture(definition(130000, "ISO", 9));
    assertRejectedWithoutWrite(fixture, 130000, payload(9));
  }

  @Test
  void writePgn_single_frame_transport_rejects_oversize_without_writing() throws Exception {
    Fixture fixture = fixture(defaultDefinitions());
    assertRejectedWithoutWrite(fixture, 126993, payload(9));
  }

  @Test
  void writePgn_endpoint_failure_propagates_without_retry_or_alternative_write() throws Exception {
    Fixture fixture = fixture(defaultDefinitions());
    IOException expected = new IOException("test endpoint unavailable");
    doThrow(expected).when(fixture.endPoint).writeFrame(any(CanFrame.class));

    IOException actual = assertThrows(IOException.class,
        () -> fixture.protocol.writePgn(60928, 77, payload(8)));

    assertSame(expected, actual);
    verify(fixture.endPoint).writeFrame(any(CanFrame.class));
    verify(fixture.endPoint, never()).writeFrames(anyList());
  }

  private static void assertRejectedWithoutWrite(Fixture fixture, int pgn, byte[] data) {
    assertAll(
        () -> {
          Exception failure = assertThrows(Exception.class, () -> fixture.protocol.writePgn(pgn, 77, data));
          assertTrue(failure instanceof IOException || failure instanceof IllegalArgumentException
              || failure instanceof UnsupportedOperationException, "Expected a clean transport rejection: " + failure);
        },
        () -> assertTrue(fixture.single.isEmpty(), "No single-frame write on rejection"),
        () -> assertTrue(fixture.groups.isEmpty(), "No grouped write on rejection"));
  }

  private static Fixture fixture(String definitions) throws Exception {
    String xml = "<PGNDefinitions><PGNs>" + definitions + "</PGNs></PGNDefinitions>";
    CanbusFormatter formatter;
    try (ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
      formatter = new CanbusFormatter(input);
    }
    // The normal constructor starts daemon sessions, threads and timers. As in the adjacent
    // protocol unit tests, bypass that lifecycle but execute the real writePgn method.
    N2kProtocol protocol = mock(N2kProtocol.class, CALLS_REAL_METHODS);
    CanbusEndPoint endPoint = mock(CanbusEndPoint.class);
    List<CanFrame> single = new ArrayList<>();
    List<List<CanFrame>> groups = new ArrayList<>();
    doAnswer(call -> { single.add(call.getArgument(0)); return null; }).when(endPoint).writeFrame(any());
    doAnswer(call -> { groups.add(List.copyOf(call.getArgument(0))); return null; }).when(endPoint).writeFrames(anyList());
    setField(protocol, Protocol.class, "endPoint", endPoint);
    setField(protocol, N2kProtocol.class, "canbusAddress", 42);
    setField(protocol, N2kProtocol.class, "formatter", formatter);
    setField(protocol, N2kProtocol.class, "framePacker", new FramePacker(formatter.getParser()));
    return new Fixture(protocol, endPoint, formatter, single, groups);
  }

  private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static byte[] payload(int length) {
    byte[] data = new byte[length];
    for (int index = 0; index < length; index++) {
      data[index] = (byte) (index + 1);
    }
    return data;
  }

  private static String defaultDefinitions() {
    return definition(59904, "Single", 3) + definition(60928, "Single", 8)
        + definition(126993, "Single", 8) + definition(126996, "Fast", 134)
        + definition(126998, "Fast", 3);
  }

  private static String definition(int pgn, String type, int length) {
    return "<PGNInfo><PGN>" + pgn + "</PGN><Id>fixture" + pgn + "</Id>"
        + "<Priority>6</Priority><Type>" + type + "</Type><Complete>true</Complete>"
        + "<Length>" + length + "</Length><Fields/></PGNInfo>";
  }

  private record Fixture(N2kProtocol protocol, CanbusEndPoint endPoint, CanbusFormatter formatter,
                         List<CanFrame> single, List<List<CanFrame>> groups) { }
}
