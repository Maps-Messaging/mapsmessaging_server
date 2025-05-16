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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public class UserProperty extends MessageProperty {

  private String key;
  private String value;

  UserProperty() {
    super(MessagePropertyFactory.USER_PROPERTY, "UserProperty");
  }

  public UserProperty(String key, String value) {
    super(MessagePropertyFactory.USER_PROPERTY, "UserProperty");
    this.key = key;
    this.value = value;
  }

  @Override
  public MessageProperty instance() {
    return new UserProperty();
  }

  @Override
  public void load(Packet packet) throws MalformedException {
    key = MQTTPacket.readUTF8(packet);
    value = MQTTPacket.readUTF8(packet);
  }

  @Override
  public void pack(Packet packet) {
    MQTTPacket.writeUTF8(packet, key);
    MQTTPacket.writeUTF8(packet, value);
  }

  @Override
  public boolean allowDuplicates() {
    return true;
  }

  public String getUserPropertyName() {
    return key;
  }

  public void setUserPropertyName(String name) {
    key = name;
  }

  public String getUserPropertyValue() {
    return value;
  }

  public void setUserPropertyValue(String value) {
    this.value = value;
  }

  @Override
  public int getSize() {
    return 4 + key.length() + value.length();
  }

  @Override
  public String toString() {
    return super.toString() + " Key:" + key + " Value:" + value;
  }
}
