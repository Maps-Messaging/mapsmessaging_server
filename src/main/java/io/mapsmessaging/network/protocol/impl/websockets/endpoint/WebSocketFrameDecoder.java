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

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class WebSocketFrameDecoder {

  static final int CONTINUATION = 0x0;
  static final int TEXT = 0x1;
  static final int BINARY = 0x2;
  static final int CLOSE = 0x8;
  static final int PING = 0x9;
  static final int PONG = 0xA;

  private static final int SHORT_LENGTH = 126;
  private static final int LONG_LENGTH = 127;
  private static final long MAX_PAYLOAD_LENGTH = Integer.MAX_VALUE;

  private final byte[] maskKey = new byte[4];

  private DecodeState state = DecodeState.FIRST_BYTE;
  private boolean finish;
  private boolean masked;
  private int opcode;
  private int lengthIndicator;
  private int extendedLengthBytes;
  private int extendedLengthRead;
  private long payloadLength;
  private long payloadRead;
  private int maskRead;
  private byte[] controlPayload;
  private int fragmentedOpcode = -1;
  private boolean validateTextPayload;
  private int utf8ContinuationBytes;
  private int utf8CodePoint;
  private int utf8MinimumCodePoint;
  private boolean closeReceived;

  int decode(Packet source, Packet destination, Listener listener) throws IOException {
    int initialPosition = destination.position();

    while (source.hasRemaining()) {
      switch (state) {
        case FIRST_BYTE -> readFirstByte(source);
        case SECOND_BYTE -> readSecondByte(source);
        case EXTENDED_LENGTH -> readExtendedLength(source);
        case MASK -> readMask(source, listener);
        case PAYLOAD -> {
          if (!readPayload(source, destination, listener)) {
            return destination.position() - initialPosition;
          }
        }
      }
    }
    return destination.position() - initialPosition;
  }

  boolean isCloseReceived() {
    return closeReceived;
  }

  private void readFirstByte(Packet source) throws IOException {
    int value = source.getByte();
    if ((value & 0x70) != 0) {
      throw protocolError("RSV bits are set without a negotiated extension");
    }

    finish = (value & 0x80) != 0;
    opcode = value & 0x0F;
    if (!isSupportedOpcode(opcode)) {
      throw protocolError("Unsupported WebSocket opcode " + opcode);
    }
    state = DecodeState.SECOND_BYTE;
  }

  private void readSecondByte(Packet source) throws IOException {
    int value = source.getByte();
    masked = (value & 0x80) != 0;
    if (!masked) {
      throw protocolError("Client WebSocket frames must be masked");
    }

    lengthIndicator = value & 0x7F;
    if (isControlFrame()) {
      if (!finish) {
        throw protocolError("Control frames must not be fragmented");
      }
      if (lengthIndicator > 125) {
        throw protocolError("Control frame payload exceeds 125 bytes");
      }
    }

    if (lengthIndicator < SHORT_LENGTH) {
      payloadLength = lengthIndicator;
      state = DecodeState.MASK;
    } else {
      extendedLengthBytes = lengthIndicator == SHORT_LENGTH ? 2 : 8;
      extendedLengthRead = 0;
      payloadLength = 0;
      state = DecodeState.EXTENDED_LENGTH;
    }
  }

  private void readExtendedLength(Packet source) throws IOException {
    while (source.hasRemaining() && extendedLengthRead < extendedLengthBytes) {
      int value = source.getByte();
      if (extendedLengthBytes == 8 && extendedLengthRead == 0 && (value & 0x80) != 0) {
        throw protocolError("WebSocket payload length exceeds 63 bits");
      }
      payloadLength = (payloadLength << 8) | value;
      extendedLengthRead++;
    }

    if (extendedLengthRead == extendedLengthBytes) {
      if (lengthIndicator == SHORT_LENGTH && payloadLength < SHORT_LENGTH) {
        throw protocolError("Non-minimal 16-bit WebSocket payload length");
      }
      if (lengthIndicator == LONG_LENGTH && payloadLength < 65_536) {
        throw protocolError("Non-minimal 64-bit WebSocket payload length");
      }
      if (payloadLength > MAX_PAYLOAD_LENGTH) {
        throw new WebSocketProtocolException(1009,
            "WebSocket payload exceeds supported maximum of " + MAX_PAYLOAD_LENGTH + " bytes");
      }
      state = DecodeState.MASK;
    }
  }

  private void readMask(Packet source, Listener listener) throws IOException {
    while (source.hasRemaining() && maskRead < maskKey.length) {
      maskKey[maskRead++] = source.get();
    }

    if (maskRead == maskKey.length) {
      beginPayload();
      if (payloadLength == 0) {
        completeFrame(listener);
      }
    }
  }

  private void beginPayload() throws IOException {
    if (opcode == CONTINUATION) {
      if (fragmentedOpcode == -1) {
        throw protocolError("Continuation frame received without an open fragmented message");
      }
      validateTextPayload = fragmentedOpcode == TEXT;
    } else if (opcode == TEXT || opcode == BINARY) {
      if (fragmentedOpcode != -1) {
        throw protocolError("New data frame received before fragmented message completion");
      }
      validateTextPayload = opcode == TEXT;
    } else {
      validateTextPayload = false;
    }

    controlPayload = isControlFrame() ? new byte[(int) payloadLength] : null;
    state = DecodeState.PAYLOAD;
  }

  private boolean readPayload(Packet source, Packet destination, Listener listener) throws IOException {
    if (isControlFrame()) {
      while (source.hasRemaining() && payloadRead < payloadLength) {
        controlPayload[(int) payloadRead] = unmask(source.get(), payloadRead);
        payloadRead++;
      }
    } else {
      while (source.hasRemaining() && destination.hasRemaining() && payloadRead < payloadLength) {
        byte value = unmask(source.get(), payloadRead);
        if (validateTextPayload) {
          validateUtf8Byte(value);
        }
        destination.put(value);
        payloadRead++;
      }
      if (payloadRead < payloadLength && !destination.hasRemaining()) {
        return false;
      }
    }

    if (payloadRead == payloadLength) {
      completeFrame(listener);
    }
    return true;
  }

  private void completeFrame(Listener listener) throws IOException {
    if (validateTextPayload && finish && utf8ContinuationBytes != 0) {
      throw protocolError("Text message ends with an incomplete UTF-8 sequence");
    }

    switch (opcode) {
      case TEXT, BINARY -> {
        if (!finish) {
          fragmentedOpcode = opcode;
        }
      }
      case CONTINUATION -> {
        if (finish) {
          fragmentedOpcode = -1;
          resetUtf8Validation();
        }
      }
      case PING -> {
        if (listener != null) {
          listener.onPing(Arrays.copyOf(controlPayload, controlPayload.length));
        }
      }
      case PONG -> {
        if (listener != null) {
          listener.onPong(Arrays.copyOf(controlPayload, controlPayload.length));
        }
      }
      case CLOSE -> {
        validateClosePayload(controlPayload);
        closeReceived = true;
        if (listener != null) {
          listener.onClose(Arrays.copyOf(controlPayload, controlPayload.length));
        }
      }
      default -> throw protocolError("Unsupported WebSocket opcode " + opcode);
    }

    if ((opcode == TEXT || opcode == BINARY) && finish) {
      resetUtf8Validation();
    }
    resetFrame();
  }

  private void validateUtf8Byte(byte value) throws IOException {
    int unsigned = value & 0xFF;
    if (utf8ContinuationBytes == 0) {
      if (unsigned <= 0x7F) {
        return;
      }
      if (unsigned >= 0xC2 && unsigned <= 0xDF) {
        utf8ContinuationBytes = 1;
        utf8CodePoint = unsigned & 0x1F;
        utf8MinimumCodePoint = 0x80;
        return;
      }
      if (unsigned >= 0xE0 && unsigned <= 0xEF) {
        utf8ContinuationBytes = 2;
        utf8CodePoint = unsigned & 0x0F;
        utf8MinimumCodePoint = 0x800;
        return;
      }
      if (unsigned >= 0xF0 && unsigned <= 0xF4) {
        utf8ContinuationBytes = 3;
        utf8CodePoint = unsigned & 0x07;
        utf8MinimumCodePoint = 0x10000;
        return;
      }
      throw invalidPayload("Invalid UTF-8 leading byte in text message");
    }

    if ((unsigned & 0xC0) != 0x80) {
      throw invalidPayload("Invalid UTF-8 continuation byte in text message");
    }
    utf8CodePoint = (utf8CodePoint << 6) | (unsigned & 0x3F);
    utf8ContinuationBytes--;
    if (utf8ContinuationBytes == 0) {
      if (utf8CodePoint < utf8MinimumCodePoint
          || utf8CodePoint > 0x10FFFF
          || (utf8CodePoint >= 0xD800 && utf8CodePoint <= 0xDFFF)) {
        throw invalidPayload("Invalid UTF-8 code point in text message");
      }
    }
  }

  private void validateClosePayload(byte[] payload) throws IOException {
    if (payload.length == 1) {
      throw protocolError("Close frame payload must be empty or contain a status code");
    }
    if (payload.length < 2) {
      return;
    }

    int code = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
    if (!isValidCloseCode(code)) {
      throw protocolError("Invalid WebSocket close status code " + code);
    }
    if (payload.length > 2) {
      validateUtf8(Arrays.copyOfRange(payload, 2, payload.length));
    }
  }

  private void validateUtf8(byte[] value) throws IOException {
    try {
      StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(value));
    } catch (CharacterCodingException e) {
      throw invalidPayload("Close frame reason is not valid UTF-8");
    }
  }

  private boolean isValidCloseCode(int code) {
    if (code < 1000 || code >= 5000) {
      return false;
    }
    return code != 1004 && code != 1005 && code != 1006 && code != 1015;
  }

  private byte unmask(byte value, long offset) {
    return (byte) (value ^ maskKey[(int) (offset & 3)]);
  }

  private boolean isControlFrame() {
    return opcode >= 0x8;
  }

  private boolean isSupportedOpcode(int candidate) {
    return candidate == CONTINUATION
        || candidate == TEXT
        || candidate == BINARY
        || candidate == CLOSE
        || candidate == PING
        || candidate == PONG;
  }

  private WebSocketProtocolException protocolError(String message) {
    return new WebSocketProtocolException(1002, "WebSocket protocol error: " + message);
  }

  private WebSocketProtocolException invalidPayload(String message) {
    return new WebSocketProtocolException(1007, "WebSocket invalid payload: " + message);
  }

  private void resetFrame() {
    state = DecodeState.FIRST_BYTE;
    finish = false;
    masked = false;
    opcode = 0;
    lengthIndicator = 0;
    extendedLengthBytes = 0;
    extendedLengthRead = 0;
    payloadLength = 0;
    payloadRead = 0;
    maskRead = 0;
    controlPayload = null;
    validateTextPayload = false;
    Arrays.fill(maskKey, (byte) 0);
  }

  private void resetUtf8Validation() {
    utf8ContinuationBytes = 0;
    utf8CodePoint = 0;
    utf8MinimumCodePoint = 0;
  }

  interface Listener {
    void onPing(byte[] payload) throws IOException;

    void onPong(byte[] payload) throws IOException;

    void onClose(byte[] payload) throws IOException;
  }

  private enum DecodeState {
    FIRST_BYTE,
    SECOND_BYTE,
    EXTENDED_LENGTH,
    MASK,
    PAYLOAD
  }
}
