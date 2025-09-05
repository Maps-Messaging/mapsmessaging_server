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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;

public class PacketFactory {

  private final MQTTProtocol protocolImpl;

  public PacketFactory(MQTTProtocol protocolImpl) {
    this.protocolImpl = protocolImpl;
  }

  //
  // First Byte
  //
  // Bits 0:3 Must be 0
  // Bits 4:7 MQTT Packet ID
  //
  // Second Byte
  // Remaining length
  //
  public MQTTPacket parseFrame(Packet packet) throws EndOfBufferException, MalformedException {
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

  protected MQTTPacket create(int packetId, byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {

    if (protocolImpl.getEndPoint().isClient()) {
      return createClientPacket(packetId, fixedHeader, remainingLen, packet);
    } else {
      return createServerPacket(packetId, fixedHeader, remainingLen, packet);
    }
  }

  private MQTTPacket createClientPacket(int packetId, byte fixedHeader, long remainingLen, Packet packet) throws MalformedException {
    switch (packetId) {
      case MQTTPacket.CONNACK:
        return new ConnAck(packet);

      case MQTTPacket.SUBACK:
        return new SubAck(fixedHeader, remainingLen, packet);

      case MQTTPacket.PINGRESP:
        return new PingResp(fixedHeader, remainingLen);

      case MQTTPacket.PUBLISH:
        return new Publish(fixedHeader, remainingLen, packet, protocolImpl.getMaximumBufferSize());

      default:
        throw new MalformedException("Unexpected packet received::" + packetId);
    }
  }

  private MQTTPacket createServerPacket(int packetId, byte fixedHeader, long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    switch (packetId) {
      case MQTTPacket.CONNECT:
        return new Connect(fixedHeader, remainingLen, packet);

      case MQTTPacket.DISCONNECT:
        return new Disconnect();

      case MQTTPacket.PINGREQ:
        return new PingReq(fixedHeader, remainingLen);

      case MQTTPacket.SUBSCRIBE:
        return new Subscribe(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBLISH:
        return new Publish(fixedHeader, remainingLen, packet, protocolImpl.getMaximumBufferSize());

      case MQTTPacket.PUBCOMP:
        return new PubComp(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBACK:
        return new PubAck(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBREL:
        return new PubRel(fixedHeader, remainingLen, packet);

      case MQTTPacket.PUBREC:
        return new PubRec(fixedHeader, remainingLen, packet);

      case MQTTPacket.UNSUBSCRIBE:
        return new Unsubscribe(fixedHeader, remainingLen, packet);

      default:
        throw new MalformedException("Unexpected packet received");
    }
  }
}
