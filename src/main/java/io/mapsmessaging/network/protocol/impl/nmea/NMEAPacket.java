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
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NMEAPacket {

  @Getter
  private final String sentence;
  @Getter
  private final String name;

  private final List<String> entries;

  public NMEAPacket(Packet packet) throws IOException {
    int pos = packet.position();
    int startPos = skipToStart(packet);
    if (startPos != -1) {
      try {
        sentence = parseSentence(packet);
        entries = new ArrayList<>(Arrays.asList(sentence.split(",")));
        name = entries.remove(0);
      } catch (EndOfBufferException e) {
        packet.position(pos);
        throw e;
      }
    } else {
      name = "";
      sentence = "";
      entries = new ArrayList<>();
    }
  }

  public NMEAPacket(String sentence) {
    this.sentence = sentence;
    entries = new ArrayList<>(Arrays.asList(sentence.split(",")));
    name = entries.remove(0);
  }


  public Iterator<String> getEntries() {
    return entries.iterator();
  }

  private int skipToStart(Packet packet) {
    int startPos = -1;
    while (packet.hasRemaining() && startPos == -1) { // Read and ignore to Start char
      byte val = packet.get();
      if (val == Constants.START) {
        startPos = packet.position();
      }
    }
    return startPos;
  }
  private String parseSentence(Packet packet) throws IOException {
    int checksum = 0;
    StringBuilder sentenceBuilder = new StringBuilder();

    while (packet.hasRemaining()) {
      byte value = packet.get();

      if (value == Constants.START) {
        continue;
      }

      if (value == Constants.CHECKSUM) {
        int sentChecksum = readChecksum(packet);
        consumeOptionalLineEnding(packet);

        if (sentChecksum == checksum) {
          return sentenceBuilder.toString();
        }

        throw new IOException("Invalid checksum calculated");
      }

      if (value == Constants.CR || value == Constants.LF) {
        throw new EndOfBufferException();
      }

      sentenceBuilder.append((char) value);
      checksum ^= value;
    }

    throw new EndOfBufferException();
  }

  private int readChecksum(Packet packet) throws IOException {
    byte first = readRequiredByte(packet, "Unexpected end of packet while reading checksum");
    byte second = readRequiredByte(packet, "Unexpected end of packet while reading checksum");

    int high = Character.digit((char) first, 16);
    int low = Character.digit((char) second, 16);

    if (high < 0 || low < 0) {
      throw new IOException("Invalid checksum characters");
    }

    return (high << 4) | low;
  }

  private byte readRequiredByte(Packet packet, String message) throws IOException {
    if (!packet.hasRemaining()) {
      throw new EndOfBufferException(message);
    }

    return packet.get();
  }

  private void consumeOptionalLineEnding(Packet packet) throws IOException {
    if (!packet.hasRemaining()) {
      return;
    }

    byte value = packet.peek();

    if (value != Constants.CR && value != Constants.LF) {
      return;
    }

    packet.get();

    if (!packet.hasRemaining()) {
      return;
    }

    byte nextValue = packet.peek();

    if (nextValue == Constants.CR || nextValue == Constants.LF) {
      packet.get();
    }
  }

}
