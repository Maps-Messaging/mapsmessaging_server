/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PacketFactory;
import java.io.IOException;

public class PacketFactoryV2 extends PacketFactory {

  @Override
  public MQTT_SNPacket parseFrame(Packet packet) throws IOException {
    int packetLength = MQTT_SNPacket.readLength(packet);
    int type = packet.get();
    return create(type, packetLength, packet);
  }

  @Override
  protected MQTT_SNPacket create(int type, int length, Packet packet) throws IOException {
    switch (type) {
      case MQTT_SNPacket.CONNECT:
        return new Connect(packet, length);

      case MQTT_SNPacket.DISCONNECT:
        return new Disconnect(packet, length);

      case MQTT_SNPacket.SUBSCRIBE:
        return new Subscribe(packet);

      case MQTT_SNPacket.UNSUBSCRIBE:
        return new Unsubscribe(packet);

      case MQTT_SNPacket.PINGREQ:
        return new PingRequest(packet, length);

      case MQTT_SNPacket.PINGRESP:
        return new PingResponse();

      case MQTT_SNPacket.GWINFO:
        return new GatewayInfo(packet, length);

      case MQTT_SNPacket.REGISTER:
        return new Register(packet, length);

      case MQTT_SNPacket.REGACK:
        return new RegisterAck(packet, length);

      case MQTT_SNPacket.PUBLISH:
        return new Publish(packet, length);

      case MQTT_SNPacket.PUBACK:
        return new PubAck(packet);

      case MQTT_SN_2_Packet.AUTH:
        return new Auth(packet, length);

      default:
        return super.create( type,  length,  packet);
    }
  }
}