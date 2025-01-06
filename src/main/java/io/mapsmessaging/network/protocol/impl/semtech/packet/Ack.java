/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.semtech.packet;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.network.io.Packet;
import java.net.SocketAddress;
import lombok.Getter;

public abstract class Ack extends SemTechPacket {

  @Getter
  private final int token;
  private final int type;

  protected Ack(int token, int type, SocketAddress fromAddress) {
    super(fromAddress);
    this.token = token;
    this.type = type;
  }

  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(type);
    return 4;
  }
}
