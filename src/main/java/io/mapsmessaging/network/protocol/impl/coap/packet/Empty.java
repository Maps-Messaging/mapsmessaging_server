/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.EMPTY;

public class Empty extends BasePacket {

  public Empty(Packet packet) {
    super(EMPTY, packet);
  }

  public Empty(int messageId){
    super(EMPTY, TYPE.RST, Code.EMPTY, 1, messageId, new byte[0]);
  }

  public Empty(TYPE type, Code code, int version, int messageId, byte[] token){
    super(EMPTY, type, code, version, messageId, token);
  }
}