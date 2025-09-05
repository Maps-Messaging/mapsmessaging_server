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

/**
 * http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718033
 */
public class ConnAck extends MQTTPacket {

  public static final byte SUCCESS = 0;
  public static final byte INVALID_PROTOCOL = 1;
  public static final byte IDENTIFIER_REJECTED = 2;
  public static final byte SERVER_UNAVAILABLE = 3;
  public static final byte BAD_USERNAME_PASSWORD = 4;
  public static final byte NOT_AUTHORISED = 5;

  private byte response;
  private boolean isPresent;

  public ConnAck() {
    super(CONNACK);
  }

  public ConnAck(Packet packet) {
    super(CONNACK);
    isPresent = packet.get() != 0;
    response = packet.get();
  }

  @Override
  public int packFrame(Packet packet) {
    packControlByte(packet, 0);
    packet.put((byte) 2);
    if (isPresent) {
      packet.put((byte) 1);
    } else {
      packet.put((byte) 0);
    }
    packet.put(response);
    return 2;
  }

  public byte getResponseCode() {
    return response;
  }

  public void setResponseCode(byte response) {
    this.response = response;
  }

  public String toString() {
    StringBuilder sb =
        new StringBuilder("MQTT ConAck [ is present:" + isPresent + " Response Code:");
    switch (response) {
      case SUCCESS:
        sb.append("Success");
        break;
      case INVALID_PROTOCOL:
        sb.append("Invalid Protocol");
        break;

      case IDENTIFIER_REJECTED:
        sb.append("Identifier Rejected");
        break;

      case SERVER_UNAVAILABLE:
        sb.append("Server Unavailable");
        break;

      case BAD_USERNAME_PASSWORD:
        sb.append("Bad Username or Password");
        break;

      case NOT_AUTHORISED:
        sb.append("Not Authorized on server");
        break;

      default:
        sb.append("Unknown");
    }
    sb.append("]");
    return sb.toString();
  }

  public void setRestoredFlag(boolean restored) {
    isPresent = restored;
  }
}
