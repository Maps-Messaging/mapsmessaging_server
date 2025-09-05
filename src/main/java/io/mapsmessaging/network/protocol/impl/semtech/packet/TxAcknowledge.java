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
import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.TX_ACK;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

/**
 * ### 5.5. TX_ACK packet ###
 *
 * That packet type is used by the gateway to send a feedback to the server to inform if a downlink request has been accepted or rejected by the gateway. The datagram may
 * optionally contain a JSON string to give more details on acknowledge. If no JSON is present (empty string), this means than no error occurred.
 *
 * Bytes  | Function
 * -------|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | same token as the PULL_RESP packet to acknowledge
 * 3      | TX_ACK identifier 0x05
 * 4-11   | Gateway unique identifier (MAC address)
 * 12-end | [optional] JSON object, starting with {, ending with }, see section 6
 */
@ToString
public class TxAcknowledge extends SemTechPacket {

  @Getter
  private final int token;
  @Getter
  private final byte[] gatewayIdentifier;
  @Getter
  private final String jsonObject;

  public TxAcknowledge(int token, byte[] gatewayIdentifier, String jsonObject, SocketAddress fromAddress){
    super(fromAddress);
    this.token = token;
    this.gatewayIdentifier = gatewayIdentifier;
    this.jsonObject = jsonObject;

  }

  public TxAcknowledge(int token, Packet packet) {
    super(packet.getFromAddress());
    this.token = token;
    gatewayIdentifier = new byte[8];
    packet.get(gatewayIdentifier);
    if (packet.available() > 0) {
      byte[] tmp = new byte[packet.available()];
      packet.get(tmp);
      jsonObject = new String(tmp);
    } else {
      jsonObject = "";
    }
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(TX_ACK);
    packet.put(gatewayIdentifier);
    if (!jsonObject.isEmpty()) {
      packet.put(jsonObject.getBytes(StandardCharsets.UTF_8));
    }
    return 12 + jsonObject.length();
  }


  @Override
  public int getIdentifier() {
    return TX_ACK;
  }
}
