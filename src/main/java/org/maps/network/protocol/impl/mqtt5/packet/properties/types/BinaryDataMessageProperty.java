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

package org.maps.network.protocol.impl.mqtt5.packet.properties.types;

import org.maps.network.io.Packet;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt5.packet.properties.MessageProperty;

public abstract class BinaryDataMessageProperty extends MessageProperty {

  protected byte[] value;

  protected BinaryDataMessageProperty(int id, String name) {
    super(id, name);
  }

  @Override
  public void load(Packet packet) {
    value = MQTTPacket.readBuffer(packet);
  }

  @Override
  public void pack(Packet packet) {
    MQTTPacket.writeBuffer(value, packet);
  }

  @Override
  public int getSize() {
    return value.length + 2;
  }

  @Override
  public String toString() {
    return super.toString() + ":" + value;
  }
}
