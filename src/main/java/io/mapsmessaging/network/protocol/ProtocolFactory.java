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

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.proxy.*;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProtocolFactory implements ServiceManager {

  @Getter
  @Setter
  private static List<ProtocolImplFactory> protocolServiceList;
  private final String protocols;
  private final List<ProxyProtocol> proxyProtocols;

  public ProtocolFactory(String protocols) {
    this.protocols = protocols.toLowerCase();
    proxyProtocols = List.of(
        new ProxyProtocolV1(),
        new ProxyProtocolV2()
    );
  }

  public ProtocolImplFactory getBoundedProtocol() {
    for (ProtocolImplFactory protocol : protocolServiceList) {
      if (protocols.equalsIgnoreCase(protocol.getName())) {
        return protocol;
      }
    }
    return null;
  }

  public DetectedProtocol detect(Packet packet, ProxyProtocolMode proxyProtocol) throws IOException {
    int potential = 0;
    int failed = 0;
    StringBuilder sb = new StringBuilder();
    ProxyProtocolInfo proxyProtocolInfo = (proxyProtocol != ProxyProtocolMode.DISABLED) ? detectProxy(packet) : null;
    if(proxyProtocolInfo == null && proxyProtocol == ProxyProtocolMode.REQUIRED) {
      throw new IOException("PROXY header not detected in incoming packet but end point is configured to require it");
    }
    for (ProtocolImplFactory protocol : protocolServiceList) {
      if ((protocols.contains("all") &&
          !protocol.getName().equalsIgnoreCase("echo") &&
          !protocol.getName().equalsIgnoreCase("NMEA-0183")) ||
          protocols.contains(protocol.getName().toLowerCase())) {
        sb.append(protocol.getName()).append(",");
        potential++;
        try {
          if (protocol.detect(packet)) {
            return new DetectedProtocol(proxyProtocolInfo, protocol);
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

  private ProxyProtocolInfo detectProxy(Packet packet) throws UnknownHostException {
    for (ProxyProtocol proxyProtocol : proxyProtocols) {
      if (proxyProtocol.matches(packet)) {
        return proxyProtocol.parse(packet);
      }
    }
    packet.getRawBuffer().reset();
    return null;
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>(protocolServiceList);
    return service.listIterator();
  }
}
