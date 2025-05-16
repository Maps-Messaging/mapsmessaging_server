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

package io.mapsmessaging.network.protocol.impl.semtech.packet;

import io.mapsmessaging.network.io.Packet;
import lombok.ToString;

import java.net.SocketAddress;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_RESPONSE;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

/**
 * ### 5.4. PULL_RESP packet ###
 *
 * That packet type is used by the server to send RF packets and associated metadata that will have to be emitted by the gateway.
 *
 * Bytes  | Function
 * -------|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | random token 3      | PULL_RESP identifier 0x03
 * 4-end  | JSON object, starting with {, ending with }, see section 6
 */
@ToString
public class PullResponse extends SemTechPacket {

  private final int token;
  private final byte[] jsonObject;

  public PullResponse(int token, Packet packet) {
    super(packet.getFromAddress());
    this.token = token;
    packet.position(4);
    jsonObject = new byte[packet.available()];
    packet.get(jsonObject);
  }

  public PullResponse(int token, byte[] jsonObject, SocketAddress fromAddress) {
    super(fromAddress);
    this.token = token;
    this.jsonObject = jsonObject;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(PULL_RESPONSE);
    packet.put(jsonObject);
    return 4 + jsonObject.length;
  }

  @Override
  public int getIdentifier() {
    return PULL_RESPONSE;
  }
}
