/*
 *
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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Validates the AMQP 1.0 transport envelope before bytes enter the Proton decoder.
 * Validation is incremental and does not consume the caller's ByteBuffer.
 */
public final class AmqpInboundFrameValidator {

  private static final int AMQP = 0;
  private static final int SASL = 3;
  private static final int HEADER_LENGTH = 8;
  private static final int PRE_OPEN_MAX_FRAME = 512;
  private static final byte[] MAGIC = {'A', 'M', 'Q', 'P'};

  private final byte[] protocolHeader = new byte[HEADER_LENGTH];
  private final byte[] frameHeader = new byte[HEADER_LENGTH];
  private final byte[] openDescriptor = new byte[10];

  private Phase phase = Phase.PROTOCOL_HEADER;
  private int headerRead;
  private int frameHeaderRead;
  private int frameRemaining;
  private int bodyRead;
  private int bodyStart;
  private int descriptorRead;
  private int descriptorLength;
  private boolean sasl;
  private boolean openReceived;

  public void validate(ByteBuffer bytes, boolean saslComplete) throws IOException {
    ByteBuffer input = bytes.duplicate();
    while (input.hasRemaining()) {
      if (phase == Phase.FRAME_HEADER && frameHeaderRead == 0
          && sasl && saslComplete && (input.get(input.position()) & 0xff) == 'A') {
        // After SASL-OUTCOME the peer begins a fresh, ordinary AMQP header.
        phase = Phase.PROTOCOL_HEADER;
        headerRead = 0;
        sasl = false;
      }
      switch (phase) {
        case PROTOCOL_HEADER -> readProtocolHeader(input);
        case FRAME_HEADER -> readFrameHeader(input);
        case FRAME_BODY -> readFrameBody(input);
      }
    }
  }

  private void readProtocolHeader(ByteBuffer input) throws IOException {
    while (input.hasRemaining() && headerRead < HEADER_LENGTH) {
      protocolHeader[headerRead++] = input.get();
    }
    if (headerRead < HEADER_LENGTH) {
      return;
    }
    for (int i = 0; i < MAGIC.length; i++) {
      if (protocolHeader[i] != MAGIC[i]) {
        throw new IOException("Invalid AMQP protocol header");
      }
    }
    int id = protocolHeader[4] & 0xff;
    if ((id != AMQP && id != SASL)
        || protocolHeader[5] != 1 || protocolHeader[6] != 0 || protocolHeader[7] != 0) {
      throw new UnsupportedVersion(id == SASL ? SASL : AMQP);
    }
    sasl = id == SASL;
    if (!sasl) {
      openReceived = false;
    }
    frameHeaderRead = 0;
    phase = Phase.FRAME_HEADER;
  }

  private void readFrameHeader(ByteBuffer input) throws IOException {
    while (input.hasRemaining() && frameHeaderRead < HEADER_LENGTH) {
      frameHeader[frameHeaderRead++] = input.get();
    }
    if (frameHeaderRead < HEADER_LENGTH) {
      return;
    }
    long size = ((long) (frameHeader[0] & 0xff) << 24)
        | ((long) (frameHeader[1] & 0xff) << 16)
        | ((long) (frameHeader[2] & 0xff) << 8)
        | (frameHeader[3] & 0xff);
    int doff = frameHeader[4] & 0xff;
    int type = frameHeader[5] & 0xff;
    int channel = ((frameHeader[6] & 0xff) << 8) | (frameHeader[7] & 0xff);
    if (size < HEADER_LENGTH || doff < 2 || (long) doff * 4 > size) {
      throw new IOException("Malformed AMQP frame SIZE or DOFF");
    }
    if (type != (sasl ? 1 : 0)) {
      throw new IOException("Unexpected AMQP frame type " + type);
    }
    if (!sasl && !openReceived) {
      if (size > PRE_OPEN_MAX_FRAME) {
        throw new IOException("AMQP frame exceeds pre-OPEN maximum of 512 bytes");
      }
      if (channel != 0 || size == (long) doff * 4) {
        throw new IOException("First AMQP frame must carry OPEN on channel zero");
      }
    }
    frameRemaining = (int) size - HEADER_LENGTH;
    bodyStart = doff * 4 - HEADER_LENGTH;
    bodyRead = 0;
    descriptorRead = 0;
    descriptorLength = 0;
    phase = frameRemaining == 0 ? Phase.FRAME_HEADER : Phase.FRAME_BODY;
    if (frameRemaining == 0) {
      frameHeaderRead = 0;
    }
  }

  private void readFrameBody(ByteBuffer input) throws IOException {
    while (input.hasRemaining() && frameRemaining > 0) {
      byte value = input.get();
      if (!sasl && !openReceived && bodyRead >= bodyStart && descriptorRead < openDescriptor.length) {
        readOpenDescriptor(value);
      }
      bodyRead++;
      frameRemaining--;
    }
    if (frameRemaining == 0) {
      if (!sasl && !openReceived) {
        throw new IOException("First AMQP frame did not contain OPEN");
      }
      frameHeaderRead = 0;
      phase = Phase.FRAME_HEADER;
    }
  }

  private void readOpenDescriptor(byte value) throws IOException {
    openDescriptor[descriptorRead++] = value;
    if (descriptorRead == 1 && value != 0) {
      throw new IOException("First AMQP frame must contain OPEN");
    }
    if (descriptorRead == 2) {
      int kind = value & 0xff;
      descriptorLength = kind == 0x53 ? 3 : kind == 0x80 ? 10 : -1;
      if (descriptorLength == -1) {
        throw new IOException("First AMQP frame must contain OPEN descriptor");
      }
    }
    if (descriptorLength != 0 && descriptorRead == descriptorLength) {
      if (descriptorLength == 3) {
        if ((openDescriptor[2] & 0xff) != 0x10) {
          throw new IOException("First AMQP performative is not OPEN");
        }
      } else {
        for (int i = 2; i < 9; i++) {
          if (openDescriptor[i] != 0) {
            throw new IOException("First AMQP performative is not OPEN");
          }
        }
        if ((openDescriptor[9] & 0xff) != 0x10) {
          throw new IOException("First AMQP performative is not OPEN");
        }
      }
      openReceived = true;
    }
  }

  public static final class UnsupportedVersion extends IOException {
    private final int protocolId;

    UnsupportedVersion(int protocolId) {
      super("Unsupported AMQP protocol version");
      this.protocolId = protocolId;
    }

    public byte[] supportedHeader() {
      return new byte[] {'A', 'M', 'Q', 'P', (byte) protocolId, 1, 0, 0};
    }
  }

  private enum Phase {
    PROTOCOL_HEADER, FRAME_HEADER, FRAME_BODY
  }
}
