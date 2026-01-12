/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.NetworkInfoHelper;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The protocol is Mavlink so it makes sense
@SuppressWarnings("squid:S00101")
public class MavlinkProtocolFactory extends ProtocolImplFactory {

  private final Map<EndPoint, MavlinkInterfaceManager> mappedInterfaces;

  public MavlinkProtocolFactory() {
    super("Mavlink", "Mavlink UDP based protocol", null);
    mappedInterfaces = new ConcurrentHashMap<>();
  }

  @Override
  public String getTransportType() {
    return "udp";
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) {
    // This protocol is not constructed by a packet, rather it is bound to an EndPoint
  }

  @Override
  public void closed(EndPoint endPoint) {
    MavlinkInterfaceManager manager = mappedInterfaces.remove(endPoint);
    if (manager != null) {
      manager.close();
    }
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    int datagramSize = NetworkInfoHelper.getMTU(info);
    if (datagramSize > 0) {
      endPoint.getConfig().getEndPointConfig().setServerReadBufferSize(datagramSize * 2L);
      endPoint.getConfig().getEndPointConfig().setServerWriteBufferSize(datagramSize * 2L);
    }

    MavlinkInterfaceManager manager = new MavlinkInterfaceManager(info, endPoint);
    mappedInterfaces.put(endPoint, manager);
  }

  public void close() {
    for (MavlinkInterfaceManager managers : mappedInterfaces.values()) {
      managers.close();
    }
    mappedInterfaces.clear();
  }
}
