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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.network.io.Packet;

/**
 * Implements the STOMP Error frame as per https://stomp.github.io/stomp-specification-1.2.html#ERROR
 */
public class Error extends ServerFrame {

  private static final byte[] COMMAND = "ERROR".getBytes();

  private byte[] payload;

  @Override
  public Frame instance() {
    return new Error();
  }

  byte[] getCommand() {
    return COMMAND;
  }

  @Override
  public void packBody(Packet packet) {
    packet.put(payload);
    packet.put((byte) 0x0);
  }

  public void setContentType(String s) {
    putHeader("Content-Type", s);
  }

  public void setContent(byte[] bytes) {
    payload = bytes;
    putHeader("Content-Length", "" + (payload.length + 1));
  }

  @Override
  public String toString() {
    return "STOMP Error[ Header:" + getHeaderAsString() + "]";
  }
}
