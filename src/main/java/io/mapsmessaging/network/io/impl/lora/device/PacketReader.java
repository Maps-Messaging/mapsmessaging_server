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

package io.mapsmessaging.network.io.impl.lora.device;

import io.mapsmessaging.logging.ServerLogMessages;

import java.util.concurrent.locks.LockSupport;

public class PacketReader implements Runnable {

  private static final long LOG_DELAY = 120000;
  private final LoRaChipDevice device;
  private boolean isClosed;

  PacketReader(LoRaChipDevice device) {
    this.device = device;
    isClosed = false;
    Thread reader = new Thread(this);
    reader.setDaemon(true);
    reader.setName("LoRa_Packet_Reader_v2:" + device.getName());
    reader.start();
  }

  public void close() {
    isClosed = true;
  }

  @Override
  public void run() {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY); // We go high to ensure we don't drop packets
    byte[] workingBuffer = new byte[256];
    long lastReported = System.currentTimeMillis() + LOG_DELAY;
    try {
      while (!isClosed) {
        try {
          long flags = device.read(workingBuffer, workingBuffer.length);
          if (flags != 0) {
            // byte[0] = length of buffer
            // byte[1] = to host
            // byte[2] = from host
            // byte[3] = RSSI value
            short len = (short) (flags & 0xff);
            short from = (short) (flags >> 8 & 0xff);
            byte rssi = (byte) (flags >> 16 & 0xff);
            short to = (short) (flags >> 24 & 0xff);
            short id = (short) (flags >> 32 & 0xff);

            if (len > 0) {
              device.logger.log(ServerLogMessages.LORA_DEVICE_RECEIVED_PACKET, to, from, rssi, len, id);
              byte[] buffer = new byte[len];
              System.arraycopy(workingBuffer, 0, buffer, 0, len);
              LoRaDatagram datagram = new LoRaDatagram(to, from, rssi, buffer, id);
              device.handleIncomingPacket(datagram);
              lastReported = System.currentTimeMillis() + LOG_DELAY;
              device.updateBytesReceived(len);
            }
          } else {
            if (lastReported < System.currentTimeMillis()) {
              lastReported = System.currentTimeMillis() + LOG_DELAY;
              device.logger.log(ServerLogMessages.LORA_DEVICE_IDLE);
            }
            LockSupport.parkNanos(1000000);
          }
        } catch (Exception ex) {
          device.logger.log(ServerLogMessages.LORA_DEVICE_READ_THREAD_ERROR, device.getName(), ex);
        }
      }
    } finally {
      device.logger.log(ServerLogMessages.LORA_DEVICE_PACKET_READER_EXITED);
    }

  }
}
