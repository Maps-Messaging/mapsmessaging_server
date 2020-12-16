/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.frames;

import java.net.SocketAddress;
import java.util.Map;
import org.maps.network.io.Packet;
import org.maps.network.io.ServerPacket;
import org.maps.utilities.collections.PriorityEntry;

public abstract class ServerFrame extends Frame implements PriorityEntry, ServerPacket {

  ServerFrame() {
    super();
  }

  abstract byte[] getCommand();

  abstract void packBody(Packet packet);

  public int packFrame(Packet packet) {
    int start = packet.position();
    //
    // Pack the command
    //
    packet.put(getCommand());
    packet.put(END_OF_LINE);

    //
    // Pack the header
    //
    if (receipt != null) {
      packet.put("receipt-id".getBytes());
      packet.put(DELIMITER);
      packet.put(receipt.getBytes());
      packet.put(END_OF_LINE);
    }
    for (Map.Entry<String, String> headerEntry : getHeader().entrySet()) {
      packet.put(headerEntry.getKey().getBytes());
      packet.put(DELIMITER);
      packet.put(headerEntry.getValue().getBytes());
      packet.put(END_OF_LINE);
    }
    packet.put(END_OF_LINE);

    packBody(packet);

    packet.put((byte) 0x0);
    return packet.position() - start;
  }

  public int getPriority() {
    return 0;
  }

  public SocketAddress getFromAddress() {
    return null;
  }
}
