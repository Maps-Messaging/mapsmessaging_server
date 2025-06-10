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

package io.mapsmessaging.network.io.impl.lora.serial;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.*;

public class LoRaStreamHandler implements StreamHandler {

  private static final int LORA_MAX_BUFFER_SIZE = 256;

  private final Object writeLock;
  private final Object readLock;
  private final byte[] outputBuffer;
  private final Logger logger;

  public LoRaStreamHandler(Logger logger) {
    this.logger = logger;
    outputBuffer = new byte[LORA_MAX_BUFFER_SIZE]; // Larger than a defined LoRa buffer
    writeLock = new Object();
    readLock = new Object();
  }

  @Override
  public void close() {
    // We have nothing to close here
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    synchronized (readLock) {
      packet.clear();
      //
      // Skip any characters before the START character
      //
      try {
        int val = input.read();
        while (val != Constants.START_FRAME && val != -1) {
          val = input.read();
        }
      } catch (SerialPortTimeoutException e) {
        return parseInput(input, packet);
      }
      byte command = (byte) input.read();
      if (command == -1) {
        throw new IOException("End Of Stream reached");
      }
      byte len = (byte) (input.read() & 0xff);
      //
      // Validate the command is known
      //
      if ((command >= START_RANGE &&
          command <= END_RANGE) ||
          command == SUCCESSFUL ||
          command == FAILURE) {
        packet.put(command);
        packet.put(len);
        for (int x = 0; x < len; x++) {
          packet.put((byte) input.read());
        }
        if (input.read() != Constants.END_FRAME) {
          packet.flip();
          logger.log(ServerLogMessages.LORA_GATEWAY_FRAMING_ERROR, packet);
          return parseInput(input, packet);
        }
        return len + 2;
      } else {
        logger.log(ServerLogMessages.LORA_GATEWAY_INVALID_COMMAND, command);
        return parseInput(input, packet);
      }
    }
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) throws IOException {
    outputBuffer[0] = Constants.START_FRAME;
    int len = Math.min(packet.available(), outputBuffer.length); // We assume a fully formed packet here
    int index = 1;
    while (packet.hasRemaining()) {
      outputBuffer[index] = packet.get();
      index++;
    }
    outputBuffer[index] = Constants.END_FRAME;
    index++;
    synchronized (writeLock) {
      output.write(outputBuffer, 0, index);
      output.flush();
    }
    return len + 2;
  }
}