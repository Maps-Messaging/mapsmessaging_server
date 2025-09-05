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
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationData;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;

public class Auth5 extends MQTTPacket5 {

  private byte reasonCode;

  public Auth5() {
    super(AUTH);
  }

  public Auth5(byte fixedHeader, long remainingLen, Packet packet)
      throws MalformedException, EndOfBufferException {
    super((fixedHeader >> 4)&0xf);
    if ((fixedHeader & 0xf) != 0) {
      throw new MalformedException("Auth: Reserved bits in command byte not 0");
    }
    if (remainingLen > 0) {
      reasonCode = packet.get();
      if (remainingLen >= 2) {
        loadProperties(packet);
      }
    }
  }

  public Auth5(byte reasonCode, String authMethod, byte[] clientChallenge) {
    super(AUTH);
    this.reasonCode = reasonCode;
    properties.add(new AuthenticationMethod(authMethod));
    properties.add(new AuthenticationData(clientChallenge));
  }

  @Override
  public int packFrame(Packet packet) {
    int len = propertiesSize();
    int variableLen = (len + lengthSize(len)) + 1;

    packControlByte(packet, 0);
    writeVariableInt(packet, variableLen);
    packet.put(reasonCode);
    packProperties(packet, len);
    return (variableLen + 2);
  }

  public byte getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(byte reasonCode) {
    this.reasonCode = reasonCode;
  }
}
