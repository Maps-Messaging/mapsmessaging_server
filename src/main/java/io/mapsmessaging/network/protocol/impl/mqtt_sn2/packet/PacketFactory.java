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

package io.mapsmessaging.network.protocol.impl.mqtt_sn2.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;

public class PacketFactory {

  public MQTT_SN_2_Packet parseFrame(Packet packet) throws IOException {
    int packetLength = MQTT_SN_2_Packet.readLength(packet);
    int type = packet.get();
    return create(type, packetLength, packet);
  }

  private MQTT_SN_2_Packet create(int type, int length, Packet packet) throws IOException {
    switch (type) {
      case MQTT_SN_2_Packet.CONNECT:
        return new Connect(packet, length);

      case MQTT_SN_2_Packet.DISCONNECT:
        return new Disconnect(packet, length);

      case MQTT_SN_2_Packet.SUBSCRIBE:
        return new Subscribe(packet);

      case MQTT_SN_2_Packet.UNSUBSCRIBE:
        return new Unsubscribe(packet);

      case MQTT_SN_2_Packet.PINGREQ:
        return new PingRequest(packet, length);

      case MQTT_SN_2_Packet.PINGRESP:
        return new PingResponse();

      case MQTT_SN_2_Packet.ADVERTISE:
        return new Advertise(packet, length);

      case MQTT_SN_2_Packet.GWINFO:
        return new GatewayInfo(packet, length);

      case MQTT_SN_2_Packet.SEARCHGW:
        return new SearchGateway(packet);

      case MQTT_SN_2_Packet.WILLTOPIC:
        return new WillTopic(packet, length);

      case MQTT_SN_2_Packet.WILLMSG:
        return new WillMessage(packet, length);

      case MQTT_SN_2_Packet.REGISTER:
        return new Register(packet, length);

      case MQTT_SN_2_Packet.REGACK:
        return new RegisterAck(packet, length);

      case MQTT_SN_2_Packet.PUBLISH:
        return new Publish(packet, length);

      case MQTT_SN_2_Packet.PUBACK:
        return new PubAck(packet);

      case MQTT_SN_2_Packet.PUBREC:
        return new PubRec(packet);

      case MQTT_SN_2_Packet.PUBCOMP:
        return new PubComp(packet);

      case MQTT_SN_2_Packet.PUBREL:
        return new PubRel(packet);

      case MQTT_SN_2_Packet.WILLTOPICUPD:
        return new WillTopicUpdate(packet, length);

      case MQTT_SN_2_Packet.WILLMSGUPD:
        return new WillMessageUpdate(packet, length);

      case MQTT_SN_2_Packet.AUTH:
        return new Auth(packet, length);

      default:
    }

    return null;
  }
}
