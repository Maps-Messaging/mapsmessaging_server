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

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;

import java.io.IOException;

public class WebSocketPacket extends Packet {

  private WebSocketHeader header;

  public WebSocketPacket(int size) {
    super(size, false);
    header = new WebSocketHeader();
  }

  public void pack(Packet packet) {
    header = new WebSocketHeader();
    header.setLength(packet.available());
    header.setOpCode((byte) WebSocketHeader.BINARY);
    header.setFinish(true);
    header.setCompleted(true);
    header.packHeader(this);
  }

  public void pack() {
    header = new WebSocketHeader();
    header.setLength(0);
    header.packHeader(this);
  }


  @Override
  public Packet clear() {
    header.reset();
    super.clear();
    return this;
  }

  public void parse() throws IOException {
    if (!header.isCompleted()) {
      header.parse(this);
    }
  }

  public WebSocketHeader getHeader() {
    return header;
  }

  public int copy(Packet packet) {
    int idx = 0;
    byte[] maskKey = header.getMaskKey();
    while (hasRemaining() && idx < header.getLength()) {
      packet.put((byte) (get() ^ maskKey[idx % 4]));
      idx++;
    }
    return idx;
  }
}
