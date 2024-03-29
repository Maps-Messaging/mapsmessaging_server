/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.types;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;

public abstract class UTF8MessageProperty extends MessageProperty {

  protected String value;

  protected UTF8MessageProperty(int id, String name) {
    super(id, name);
  }

  @Override
  public void load(Packet packet) throws MalformedException {
    value = MQTTPacket.readUTF8(packet);
  }

  @Override
  public void pack(Packet packet) {
    MQTTPacket.writeUTF8(packet, value);
  }

  @Override
  public int getSize() {
    return 2 + value.length();
  }

  @Override
  public String toString() {
    return super.toString() + ":" + value;
  }
}
