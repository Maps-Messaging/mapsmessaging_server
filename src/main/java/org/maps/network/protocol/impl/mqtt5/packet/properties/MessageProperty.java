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

package org.maps.network.protocol.impl.mqtt5.packet.properties;

import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;

public abstract class MessageProperty {

  private final int id;
  private final String name;

  protected MessageProperty(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean allowDuplicates() {
    return false;
  }

  public abstract MessageProperty instance();

  public abstract void load(Packet packet) throws MalformedException, EndOfBufferException;

  public abstract void pack(Packet packet);

  public abstract int getSize();

  @Override
  public String toString() {
    return "Id:" + id + " name:" + name;
  }
}
