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
import lombok.Getter;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PUSH_DATA;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

/**
 * ### 3.2. PUSH_DATA packet ###
 * That packet type is used by the gateway mainly to forward the RF packets received, and associated metadata, to the server.
 * Bytes  | Function
 * -------|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | random token
 * 3      | PUSH_DATA  identifier 0x00
 * 4-11   | Gateway unique identifier (MAC address)
 * 12-end | JSON object, starting with {, ending with }, see section 4
 */

public class PushData extends SemTechPacket {

  @Getter
  private final int token;
  @Getter
  private final byte[] gatewayIdentifier;
  @Getter
  private final String jsonObject;

  public PushData(int token, Packet packet) {
    super(packet.getFromAddress());
    this.token = token;

    gatewayIdentifier = new byte[8];
    packet.get(gatewayIdentifier);
    byte[] tmp = new byte[packet.available()];
    packet.get(tmp);
    jsonObject = new String(tmp);
  }

  public PushData(int token, byte[] gatewayIdentifier, String jsonObject, SocketAddress fromAddress) {
    super(fromAddress);
    this.token = token;
    this.gatewayIdentifier = gatewayIdentifier;
    this.jsonObject = jsonObject;
  }


  public boolean isValid() {
    return jsonObject.isEmpty() || (jsonObject.startsWith("{") && jsonObject.endsWith("}"));
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(PUSH_DATA);
    packet.put(gatewayIdentifier);
    packet.put(jsonObject.getBytes(StandardCharsets.UTF_8));
    return 0;
  }

  @Override
  public int getIdentifier() {
    return PUSH_DATA;
  }

  public String toString() {
    return "PushData(token=" + token + ", gatewayIdentier=[" + (dumpIdentifier()) + "], jsonObject=" + jsonObject + ")";
  }

  public String dumpIdentifier() {
    StringBuilder sb = new StringBuilder();
    for (byte b : gatewayIdentifier) {
      sb.append(String.format("%02X", b & 0xff)).append(",");
    }
    return sb.toString();
  }
}
