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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class PingResponse extends MQTT_SN_2_Packet {

  @Getter
  @Setter
  private int messagesRemaining;

  public PingResponse() {
    super(PINGRESP);
  }

  public PingResponse(int messagesRemaining) {
    super(PINGRESP);
    this.messagesRemaining = messagesRemaining;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 3);
    packet.put((byte) PINGRESP);
    packet.put((byte) messagesRemaining);
    return 3;
  }
}
