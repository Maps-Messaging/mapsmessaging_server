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

import java.net.SocketAddress;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PUSH_ACK;

/**
 * ### 3.3. PUSH_ACK packet ###
 *
 * That packet type is used by the server to acknowledge immediately all the PUSH_DATA packets received.
 *
 * Bytes  | Function
 * -------|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | same token as the PUSH_DATA packet to acknowledge
 * 3      | PUSH_ACK identifier 0x01
 */

public class PushAck extends Ack {

  public PushAck(int token, SocketAddress fromAddress) {
    super(token, PUSH_ACK, fromAddress);
  }

  @Override
  public int getIdentifier() {
    return PUSH_ACK;
  }

  public String toString() {
    return "PushAck(token=" + getToken() + ")";
  }
}
