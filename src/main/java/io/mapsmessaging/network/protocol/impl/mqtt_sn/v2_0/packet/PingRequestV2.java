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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PingRequest;
import lombok.Getter;
import lombok.ToString;

@ToString
public class PingRequestV2 extends PingRequest {

  @Getter
  private String clientId;

  @Getter
  private final int maxMessages;

  public PingRequestV2(){
    super();
    maxMessages = 0;
  }

  public PingRequestV2(Packet packet, int length) throws MalformedException {
    super();
    if (length > 2) {
      maxMessages = packet.get();
      if (length > 4) {
        clientId = MQTTPacket.readUTF8(packet);
      }
    } else {
      maxMessages = 0;
      clientId = "";
    }
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) 2);
    packet.put((byte) PINGREQ);
    return 2;
  }
}
