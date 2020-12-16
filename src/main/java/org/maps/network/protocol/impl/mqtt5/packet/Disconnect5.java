/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt5.packet;

import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;

/**
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901205
 */

public class Disconnect5 extends MQTTPacket5 {

  private StatusCode disconnectReason;

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
