
package io.mapsmessaging.network.protocol.impl.mavlink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MavlinkFrameExtractor {

  private MavlinkFrameExtractor() {}

  public static List<byte[]> extractMavlinkFrames(byte[] data) {
    List<byte[]> frames = new ArrayList<>();
    int offset = 0;

    while (offset < data.length) {
      int magic = Byte.toUnsignedInt(data[offset]);

      if (magic != 0xFE && magic != 0xFD) {
        offset++;
        continue;
      }

      if (offset + 1 >= data.length) {
        break;
      }

      int payloadLength = Byte.toUnsignedInt(data[offset + 1]);
      int frameLength;

      if (magic == 0xFE) {
        frameLength = payloadLength + 8;
      } else {
        if (offset + 2 >= data.length) {
          break;
        }

        int incompatibilityFlags = Byte.toUnsignedInt(data[offset + 2]);
        frameLength = payloadLength + 12;

        if ((incompatibilityFlags & 0x01) != 0) {
          frameLength += 13;
        }
      }

      if (offset + frameLength > data.length) {
        break;
      }

      frames.add(Arrays.copyOfRange(data, offset, offset + frameLength));
      offset += frameLength;
    }

    return frames;
  }


  public static int getSystemId(byte[] frame) {
    return switch (Byte.toUnsignedInt(frame[0])) {
      case 0xFE -> Byte.toUnsignedInt(frame[3]);
      case 0xFD -> Byte.toUnsignedInt(frame[5]);
      default -> throw new IllegalArgumentException("Not a MAVLink frame");
    };
  }
}