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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device;

import io.mapsmessaging.network.io.Packet;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

public class XmodemBinaryHandler implements ModemLineHandler {

  private static final int SOH = 0x01;
  private static final int EOT = 0x04;
  private static final int ACK = 0x06;
  private static final int NAK = 0x15;
  private static final int CAN = 0x18;

  private final Consumer<byte[]> onComplete;
  private final Consumer<Throwable> onError;
  private final ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
  private int state = 0; // 0 = wait for SOH, 1 = reading block
  private int blockNum = 1;
  private final byte[] block = new byte[133]; // SOH + blk + ~blk + 128 + checksum
  private int blockPos = 0;

  public XmodemBinaryHandler(Consumer<byte[]> onComplete, Consumer<Throwable> onError) {
    this.onComplete = onComplete;
    this.onError = onError;
  }

  @Override
  public void onData(Packet packet) {
    while (packet.hasRemaining()) {
      int b = packet.get() & 0xFF;

      if (b == EOT) {
        onComplete.accept(dataBuffer.toByteArray());
        dataBuffer.reset();
        state = 0;
        blockNum = 1;
        return;
      } else if (b == CAN) {
        onError.accept(new RuntimeException("Transmission cancelled"));
        return;
      }

      if (state == 0 && b == SOH) {
        block[0] = (byte) b;
        blockPos = 1;
        state = 1;
      } else if (state == 1) {
        block[blockPos++] = (byte) b;
        if (blockPos == 133) {
          processBlock();
          state = 0;
        }
      }
    }
  }

  private void processBlock() {
    int blkNum = block[1] & 0xFF;
    int blkNumComp = block[2] & 0xFF;

    if ((blkNum + blkNumComp) != 0xFF) {
      onError.accept(new RuntimeException("Block number mismatch"));
      return;
    }

    if (blkNum != blockNum) {
      // Duplicate or out-of-order block, ignore
      return;
    }

    byte checksum = 0;
    for (int i = 3; i < 131; i++) {
      checksum += block[i];
    }

    if (checksum != (block[131] & 0xFF)) {
      onError.accept(new RuntimeException("Checksum mismatch"));
      return;
    }

    dataBuffer.write(block, 3, 128);
    blockNum++;
  }
}
