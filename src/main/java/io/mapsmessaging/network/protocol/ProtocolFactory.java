/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProtocolFactory implements ServiceManager {

  @Getter
  @Setter
  private static List<ProtocolImplFactory> protocolServiceList;
  private final String protocols;

  public ProtocolFactory(String protocols) {
    this.protocols = protocols;
  }

  public ProtocolImplFactory getBoundedProtocol() {
    for (ProtocolImplFactory protocol : protocolServiceList) {
      if (protocols.equalsIgnoreCase(protocol.getName())) {
        return protocol;
      }
    }
    return null;
  }

  public ProtocolImplFactory detect(Packet packet) throws IOException {
    int potential = 0;
    int failed = 0;
    StringBuilder sb = new StringBuilder();
    for (ProtocolImplFactory protocol : protocolServiceList) {
      if ((protocols.contains("all") &&
          !protocol.getName().equalsIgnoreCase("echo") &&
          !protocol.getName().equalsIgnoreCase("nmea")) ||
          protocols.contains(protocol.getName().toLowerCase())) {
        sb.append(protocol.getName()).append(",");
        potential++;
        try {
          if (protocol.detect(packet)) {
            return protocol;
          } else {
            failed++;
          }
        } catch (EndOfBufferException e) {
          // Ignore, just not enough data
        }
      }
    }
    if (potential == failed) {
      throw new IOException("No known protocol detected " + packet.toString() + " <" + potential + " != " + failed + "> " + sb.toString());
    }
    return null;
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>(protocolServiceList);
    return service.listIterator();
  }
}
