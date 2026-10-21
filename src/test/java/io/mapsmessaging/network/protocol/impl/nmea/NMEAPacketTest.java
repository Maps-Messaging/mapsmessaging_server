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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NMEAPacketTest {

  @Test
  void parsesValidSentenceAndEntries() throws IOException {
    Packet packet = packet(sentence("GPGGA,1,2", "\r\n"));

    NMEAPacket nmeaPacket = new NMEAPacket(packet);

    assertEquals("GPGGA", nmeaPacket.getName());
    assertEquals("GPGGA,1,2", nmeaPacket.getSentence());

    List<String> entries = new ArrayList<>();
    nmeaPacket.getEntries().forEachRemaining(entries::add);
    assertEquals(List.of("1", "2"), entries);
  }

  @Test
  void skipsNoiseBeforeSentenceStart() throws IOException {
    Packet packet = packet("noise-before-start" + sentence("GPRMC,A,B", "\n"));

    NMEAPacket nmeaPacket = new NMEAPacket(packet);

    assertEquals("GPRMC", nmeaPacket.getName());
    assertEquals("GPRMC,A,B", nmeaPacket.getSentence());
  }

  @Test
  void leavesFollowingSentenceAvailableAfterLineEnding() throws IOException {
    String first = sentence("GPONE,1", "\r\n");
    String second = sentence("GPTWO,2", "\n");
    Packet packet = packet(first + second);

    NMEAPacket firstPacket = new NMEAPacket(packet);
    NMEAPacket secondPacket = new NMEAPacket(packet);

    assertEquals("GPONE", firstPacket.getName());
    assertEquals("GPTWO", secondPacket.getName());
    assertFalse(packet.hasRemaining());
  }

  @Test
  void rejectsIncorrectChecksum() {
    String valid = sentence("GPGGA,1,2", "\r\n");
    String invalid = valid.substring(0, valid.indexOf('*') + 1) + "00\r\n";

    assertThrows(IOException.class, () -> new NMEAPacket(packet(invalid)));
  }

  @Test
  void rejectsNonHexChecksumCharacters() {
    String invalid = "$GPGGA,1,2*XZ\r\n";

    IOException exception = assertThrows(IOException.class, () -> new NMEAPacket(packet(invalid)));

    assertEquals("Invalid checksum characters", exception.getMessage());
  }

  @Test
  void incompleteChecksumRestoresOriginalPacketPosition() {
    Packet packet = packet("$GPGGA,1,2*A");
    int originalPosition = packet.position();

    assertThrows(EndOfBufferException.class, () -> new NMEAPacket(packet));

    assertEquals(originalPosition, packet.position());
  }

  @Test
  void prematureLineEndingRestoresOriginalPacketPosition() {
    Packet packet = packet("$GPGGA,1,2\r");
    int originalPosition = packet.position();

    assertThrows(EndOfBufferException.class, () -> new NMEAPacket(packet));

    assertEquals(originalPosition, packet.position());
  }

  @Test
  void noSentenceStartProducesEmptyPacket() throws IOException {
    Packet packet = packet("not-an-nmea-sentence");

    NMEAPacket nmeaPacket = new NMEAPacket(packet);

    assertEquals("", nmeaPacket.getName());
    assertEquals("", nmeaPacket.getSentence());
    assertFalse(nmeaPacket.getEntries().hasNext());
  }

  @Test
  void stringConstructorSplitsNameAndEntriesWithoutWireValidation() {
    NMEAPacket packet = new NMEAPacket("GPGGA,one,two");

    assertEquals("GPGGA", packet.getName());
    assertEquals("GPGGA,one,two", packet.getSentence());

    List<String> entries = new ArrayList<>();
    packet.getEntries().forEachRemaining(entries::add);
    assertEquals(List.of("one", "two"), entries);
  }

  private static Packet packet(String value) {
    return new Packet(ByteBuffer.wrap(value.getBytes(StandardCharsets.US_ASCII)));
  }

  private static String sentence(String body, String lineEnding) {
    int checksum = 0;
    for (byte value : body.getBytes(StandardCharsets.US_ASCII)) {
      checksum ^= value;
    }
    return "$" + body + "*" + String.format("%02X", checksum) + lineEnding;
  }
}
