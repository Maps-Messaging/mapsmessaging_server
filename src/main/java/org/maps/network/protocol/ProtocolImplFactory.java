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

package org.maps.network.protocol;

import java.io.IOException;
import org.maps.network.io.EndPoint;
import org.maps.network.io.InterfaceInformation;
import org.maps.network.io.Packet;
import org.maps.network.protocol.detection.Detection;
import org.maps.utilities.service.Service;

public abstract class ProtocolImplFactory implements Service {

  private final String name;
  private final String description;
  private final Detection detection;

  protected ProtocolImplFactory(String name, String description, Detection detection) {
    this.name = name;
    this.description = description;
    this.detection = detection;
  }

  public String getName() {
    return name;
  }

  public String getDescription(){
    return description;
  }

  public abstract ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException;

  public abstract void create(EndPoint endPoint, Packet packet) throws IOException;

  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException { }

  public boolean detect(Packet packet) throws EndOfBufferException {
    int pos = packet.position();
    try {
      if (detection != null) {
        return detection.detected(packet);
      } else {
        return false;
      }
    } finally {
      packet.position(pos);
    }
  }
}
