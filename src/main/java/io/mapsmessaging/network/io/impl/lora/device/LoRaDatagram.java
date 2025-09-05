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

public class LoRaDatagram {

  private final int to;
  private final int from;
  private final int rssi;
  private final byte[] buffer;
  private final int id;

  public LoRaDatagram(int to, int from, int rssi, byte[] buffer, int id) {
    this.to = to;
    this.from = from;
    this.rssi = rssi;
    this.buffer = buffer;
    this.id = id;
  }

  public int getTo() {
    return to;
  }

  public int getFrom() {
    return from;
  }

  public int getRssi() {
    return rssi;
  }

  public byte[] getBuffer() {
    return buffer;
  }

  public int getId() {
    return id;
  }
}
