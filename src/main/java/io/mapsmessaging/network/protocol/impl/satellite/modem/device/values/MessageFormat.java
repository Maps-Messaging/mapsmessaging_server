/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
package io.mapsmessaging.network.protocol.impl.satellite.modem.device.values;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public enum MessageFormat {
  DATA(0) {
    @Override
    public String encode(byte[] data) {
      throw new UnsupportedOperationException("DATA mode (0) is not supported in AT command payloads.");
    }

    @Override
    public byte[] decode(String encoded) {
      throw new UnsupportedOperationException("DATA mode (0) decoding not supported.");
    }
  },

  TEXT(1) {
    @Override
    public String encode(byte[] data) {
      StringBuilder sb = new StringBuilder("\"");
      for (byte b : data) {
        int unsigned = b & 0xFF;
        if (unsigned >= 0x20 && unsigned <= 0x7E && unsigned != '\\') {
          sb.append((char) unsigned);
        } else {
          sb.append(String.format("\\%02X", unsigned));
        }
      }
      return sb.append("\"").toString();
    }


    @Override
    public byte[] decode(String encoded) {
      if (encoded.startsWith("\"")) {
        encoded = encoded.substring(1);
      }
      if (encoded.endsWith("\"")) {
        encoded = encoded.substring(0, encoded.length() - 1);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      int i = 0;
      while (i < encoded.length()) {
        char c = encoded.charAt(i);
        if (c == '\\' && i + 2 < encoded.length()) {
          String hex = encoded.substring(i + 1, i + 3);
          out.write(Integer.parseInt(hex, 16));
          i += 3; // consume escape and hex digits
        } else {
          out.write((byte) c);
          i++; // consume single char
        }
      }
      return out.toByteArray();
    }
  },

  HEX(2) {
    @Override
    public String encode(byte[] data) {
      StringBuilder sb = new StringBuilder(data.length * 2);
      for (byte b : data) {
        sb.append(String.format("%02X", b));
      }
      return sb.toString();
    }

    @Override
    public byte[] decode(String encoded) {
      int len = encoded.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(encoded.charAt(i), 16) << 4)
            + Character.digit(encoded.charAt(i + 1), 16));
      }
      return data;
    }
  },

  BASE64(3) {
    @Override
    public String encode(byte[] data) {
      return Base64.getEncoder().encodeToString(data);
    }

    @Override
    public byte[] decode(String encoded) {
      return Base64.getDecoder().decode(encoded);
    }
  };

  private final int modemCode;

  MessageFormat(int modemCode) {
    this.modemCode = modemCode;
  }

  public static MessageFormat fromCode(int code) {
    for (MessageFormat f : values()) if (f.getCode() == code) return f;
    throw new IllegalArgumentException("Unknown dataFormat: " + code);
  }

  public int getCode() {
    return modemCode;
  }

  public abstract String encode(byte[] data);

  public abstract byte[] decode(String encoded);
}
