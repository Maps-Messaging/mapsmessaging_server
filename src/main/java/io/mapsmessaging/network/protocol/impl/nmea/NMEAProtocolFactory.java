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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.NetworkInfoHelper;
import io.mapsmessaging.network.io.impl.tcp.TCPEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.impl.mavlink.MavlinkInterfaceManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NMEAProtocolFactory extends ProtocolImplFactory {

  private final Map<EndPoint, NMEAProtocol> mappedInterfaces = new ConcurrentHashMap<>();

  public NMEAProtocolFactory() {
    super("NMEA-0183", "NMEA Gateway as per https://en.wikipedia.org/wiki/NMEA_0183", new NMEAProtocolDetection());
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password, Map<String, String> topicMap) throws IOException {
    if (endPoint instanceof TCPEndPoint) {
      Packet packet = new Packet(ByteBuffer.allocate(1024));
      packet.put("?WATCH={\"enable\":true,\"nmea\":true}".getBytes(StandardCharsets.UTF_8));
      packet.flip();
      endPoint.sendPacket(packet);
      return build(endPoint, packet);
    }
    return null;
  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    build(endPoint, packet);
  }

  private Protocol build(EndPoint endPoint, Packet packet) throws IOException {
    try {
      return new NMEAProtocol(endPoint, packet);
    } catch (LoginException e) {
      // Ignore since it should just work
    }
    return null;
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    int datagramSize = NetworkInfoHelper.getMTU(info);
    if (datagramSize > 0) {
      endPoint.getConfig().getEndPointConfig().setServerReadBufferSize(datagramSize * 2L);
      endPoint.getConfig().getEndPointConfig().setServerWriteBufferSize(datagramSize * 2L);
    }
    try {
      NMEAProtocol manager = new NMEAProtocol( endPoint, null);
      mappedInterfaces.put(endPoint, manager);
    } catch (LoginException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    for (NMEAProtocol managers : mappedInterfaces.values()) {
      try {
        managers.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    mappedInterfaces.clear();
  }

  @Override
  public String getTransportType() {
    return "serial";
  }

}