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

import lombok.ToString;

import java.net.SocketAddress;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_ACK;

/**
 * ### 5.3. PULL_ACK packet ###
 * That packet type is used by the server to confirm that the network route is open and that the server can send PULL_RESP packets at any time.
 * Bytes  | Function
 * -------|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | same token as the PULL_DATA packet to acknowledge
 * 3      | PULL_ACK identifier 0x04
 */

@ToString
public class PullAck extends Ack {

  public PullAck(int token, SocketAddress address) {
    super(token, PULL_ACK, address);
  }

  @Override
  public int getIdentifier() {
    return PULL_ACK;
  }

}
