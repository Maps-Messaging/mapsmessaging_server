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
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;

public abstract class MQTTPacket5 extends MQTTPacket {

  public static final int AUTH = 15;

  protected MessageProperties properties;

  protected MQTTPacket5(int id) {
    super(id);
    properties = new MessageProperties();
  }

  public MessageProperties add(MessageProperty property) {
    properties.add(property);
    return properties;
  }

  public MessageProperties getProperties() {
    return properties;
  }

  public long loadProperties(Packet packet) throws MalformedException, EndOfBufferException {
    properties = new MessageProperties();
    return loadProperties(packet, properties);
  }

  public long loadProperties(Packet packet, MessageProperties props)
      throws MalformedException, EndOfBufferException {

    long values = MQTTPacket.readVariableInt(packet);
    if (values != 0) {
      long end = packet.position() + values;
      while (packet.position() < end) {
        byte id = packet.get();
        MessageProperty property = MessagePropertyFactory.getInstance().find(id);
        if (property != null) {
          property.load(packet);
          props.add(property);
        } else {
          throw new MalformedException("Unknown connect payload " + id);
        }
      }
    }
    return values + 1;
  }

  public static void packProperties(Packet packet, MessageProperties props, long len) {
    writeVariableInt(packet, len);
    if (len != 0) {
      for (MessageProperty property : props.values()) {
        packet.put((byte) property.getId());
        property.pack(packet);
      }
    }
  }
  public static int propertiesSize(MessageProperties props) {
    int size = 0;
    for (MessageProperty property : props.values()) {
      size += 1 + property.getSize(); // +1 for the byte identifier
    }
    return size;
  }

  public void packProperties(Packet packet, long len) {
    packProperties(packet, properties, len);
  }

  public int propertiesSize() {
    return propertiesSize(properties);
  }

  public int lengthSize(int check) {
    if (check < 0x7F) {
      return 1;
    } else if (check < 0xff7f) {
      return 2;
    } else if (check < 0xffff7f) {
      return 3;
    } else {
      return 4;
    }
  }

  @Override
  public String toString() {
    return properties.toString();
  }
}
