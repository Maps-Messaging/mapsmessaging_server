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

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901205
 */

public class Disconnect5 extends MQTTPacket5 {

  private final StatusCode disconnectReason;

  public Disconnect5(long remainingLen, Packet packet) throws MalformedException, EndOfBufferException {
    super(DISCONNECT);
    if (remainingLen != 0) {
      disconnectReason = StatusCode.getInstance(packet.get());
    } else {
      disconnectReason = StatusCode.SUCCESS;
    }
    if (remainingLen >= 2) {
      loadProperties(packet);
    }
  }

  public Disconnect5(StatusCode reasonCode) {
    super(DISCONNECT);
    disconnectReason = reasonCode;
  }

  @Override
  public String toString() {
    return "MQTTv5 Disconnect[]";
  }

  public StatusCode getDisconnectReason() {
    return disconnectReason;
  }

  @Override
  public int packFrame(Packet packet) {
    packControlByte(packet, 0);
    packet.put((byte) 1);
    packet.put(disconnectReason.getValue());
    return 3;
  }
}
