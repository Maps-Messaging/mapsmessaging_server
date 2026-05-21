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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkStreamHandlerHangTest {

  @Test
  void parseInput_canHangForever_ifInputStreamReturnsZeroLengthReads() {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v1 = buildV1Frame(
        1,
        (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 0,
        (byte) 0x12, (byte) 0x34
    );

    InputStream input = new ZeroReadBulkInputStream(v1);
    Packet packet = new Packet(256, false);

    assertThrows(AssertionFailedError.class, () ->
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
          handler.parseInput(input, packet);
        })
    );
  }

  @Test
  void parseInput_timesOut_afterConfiguredReadTimeout() {
    int timeoutMillis = 5000;
    MavlinkStreamHandler handler = new MavlinkStreamHandler(timeoutMillis);

    byte[] frame = buildV1Frame(
        10,
        (byte) 1, (byte) 2, (byte) 3, (byte) 4,
        (byte) 0x12, (byte) 0x34
    );

    InputStream input = new ZeroReadBulkInputStream(frame);
    Packet packet = new Packet(256, false);

    long start = System.nanoTime();

    IOException ex = assertThrows(IOException.class, () -> {
      handler.parseInput(input, packet);
    });

    long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;

    assertTrue(
        elapsedMillis >= timeoutMillis,
        "Elapsed " + elapsedMillis + "ms < timeout " + timeoutMillis + "ms"
    );

    assertTrue(
        elapsedMillis < timeoutMillis + 1000,
        "Elapsed " + elapsedMillis + "ms exceeded expected timeout window"
    );

    assertTrue(
        ex.getMessage().toLowerCase().contains("timed out"),
        "Expected timeout IOException, got: " + ex.getMessage()
    );
  }

  private static final class ZeroReadBulkInputStream extends InputStream {

    private final ByteArrayInputStream delegate;

    private ZeroReadBulkInputStream(byte[] data) {
      this.delegate = new ByteArrayInputStream(data);
    }

    @Override
    public int read() {
      return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) {
      // This is the whole point: read(byte[],off,len) returns 0 without EOF.
      // Your readFully() currently spins forever on this.
      return 0;
    }
  }

  private static byte[] buildV1Frame(int payloadLength, byte seq, byte sysId, byte compId, byte msgId, byte crcExtra, byte... payloadAndCrc) {
    byte[] payload;
    byte[] crc;

    if (payloadLength == 0) {
      payload = new byte[0];
      crc = new byte[]{0, 0};
    } else {
      if (payloadAndCrc.length < payloadLength + 2) {
        payload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
          payload[i] = (byte) (0xA0 + i);
        }
        crc = new byte[]{(byte) 0x12, (byte) 0x34};
      } else {
        payload = Arrays.copyOfRange(payloadAndCrc, 0, payloadLength);
        crc = Arrays.copyOfRange(payloadAndCrc, payloadLength, payloadLength + 2);
      }
    }

    int length = 2 + 5 + payloadLength + 2;

    byte[] frame = new byte[length];
    int idx = 0;

    frame[idx++] = (byte) 0xFE;                // magic
    frame[idx++] = (byte) (payloadLength & 0xFF); // len
    frame[idx++] = seq;                        // seq
    frame[idx++] = sysId;                      // sysid
    frame[idx++] = compId;                     // compid
    frame[idx++] = msgId;                      // msgid
    frame[idx++] = crcExtra;                   // "extra" (your code treats as header byte)

    System.arraycopy(payload, 0, frame, idx, payload.length);
    idx += payload.length;

    System.arraycopy(crc, 0, frame, idx, crc.length);

    return frame;
  }
}
