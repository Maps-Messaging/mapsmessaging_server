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
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;

import java.io.IOException;

public class PacketFactoryV2 extends PacketFactory {

  @Override
  protected MQTT_SNPacket create(int type, int length, Packet packet) throws IOException {
    switch (type) {
      case MQTT_SNPacket.CONNECT:
        try {
          return new Connect(packet, length);
        } catch (Exception e) {
          return getConnectError(ReasonCodes.NOT_SUPPORTED);
        }

      case MQTT_SNPacket.DISCONNECT:
        return new Disconnect(packet, length);

      case MQTT_SNPacket.SUBSCRIBE:
        return new Subscribe(packet);

      case MQTT_SNPacket.UNSUBSCRIBE:
        return new Unsubscribe(packet);

      case MQTT_SNPacket.PINGREQ:
        try {
          return new PingRequestV2(packet, length);
        } catch (Exception e) {
          return new PingResponse();
        }

      case MQTT_SNPacket.PINGRESP:
        return new PingResponse();

      case MQTT_SNPacket.GWINFO:
        return new GatewayInfo(packet, length);

      case MQTT_SNPacket.REGISTER:
        return new Register(packet, length);

      case MQTT_SNPacket.REGACK:
        return new RegisterAck(packet, length);

      case MQTT_SNPacket.PUBLISH:
        return new Publish(packet);

      case MQTT_SNPacket.PUBACK:
        return new PubAck(packet);

      case MQTT_SN_2_Packet.AUTH:
        return new Auth(packet, length);

      default:
        return super.create(type, length, packet);
    }
  }


  @Override
  public MQTT_SNPacket getConnectError(ReasonCodes reasonCode) {
    return new ConnAck(reasonCode, 0, "", false);
  }
}
