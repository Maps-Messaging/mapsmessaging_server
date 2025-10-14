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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;

public class PacketFactory5 {

  private final MQTT5Protocol protocolImpl;

  public PacketFactory5(MQTT5Protocol protocolImpl) {
    this.protocolImpl = protocolImpl;
  }

  //
  // First Byte
  //
  // Bits 0:3 Must be 0 or set depending on packet
  // Bits 4:7 MQTT Packet ID
  //
  // Second Byte
  // Remaining length
  //
  public MQTTPacket5 parseFrame(Packet packet) throws EndOfBufferException, MalformedException {
    if (packet.available() < 2) {
      throw new EndOfBufferException("Need at least 2 bytes for a valid MQTT packet");
    }

    byte fixedHeader = packet.get();
    long remainingLen = MQTTPacket.readVariableInt(packet);

    int packetId = (fixedHeader >> 4) & 0xf;
    if (packet.available() < remainingLen) {
      throw new EndOfBufferException("MQTT packet not fully loaded at this point");
    }
    return create(packetId, fixedHeader, remainingLen, packet);
  }

  protected MQTTPacket5 create(int packetId, byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {
    switch (packetId) {
      case MQTTPacket.PUBLISH:
        return new Publish5(fixedHeader, remainingLen, packet, protocolImpl.getMaxBufferSize());

      case MQTTPacket5.AUTH:
        return new Auth5(fixedHeader, remainingLen, packet);

      case MQTTPacket.CONNECT:
        return new Connect5(fixedHeader, remainingLen, packet);

      case MQTTPacket.CONNACK:
        return new ConnAck5(fixedHeader, remainingLen, packet);

      case MQTTPacket.DISCONNECT:
        return new Disconnect5(remainingLen, packet);

      case MQTTPacket.PINGREQ:
        return new PingReq5(fixedHeader, remainingLen);

      case MQTTPacket.SUBSCRIBE:
        return new Subscribe5(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBCOMP:
        return new PubComp5(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBACK:
        return new PubAck5(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBREL:
        return new PubRel5(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBREC:
        return new PubRec5(fixedHeader, remainingLen, packet);

      case MQTTPacket.UNSUBSCRIBE:
        return new Unsubscribe5(fixedHeader, remainingLen, packet);

      case MQTTPacket.UNSUBACK:
        return new UnsubAck5(fixedHeader, remainingLen, packet);

      case MQTTPacket.SUBACK:
        return new SubAck5(fixedHeader, remainingLen, packet);

      default:
        throw new MalformedException("Unexpected packet received:::" + Long.toHexString(packetId));
    }
  }
}
