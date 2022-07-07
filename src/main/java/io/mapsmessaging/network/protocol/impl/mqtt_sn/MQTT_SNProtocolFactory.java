/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The protocol is MQTT_SN so it makes sense
@java.lang.SuppressWarnings("squid:S00101")
public class MQTT_SNProtocolFactory extends ProtocolImplFactory {


  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  private final Map<EndPoint, MQTTSNInterfaceManager> mappedInterfaces;

  public MQTT_SNProtocolFactory() {
    super("MQTT-SN", "MQTT-SN UDP based protocol as per http://mqtt.org/mqtt-specification/", null);
    mappedInterfaces = new ConcurrentHashMap<>();
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) {
    // This protocol is not constructed by a packet, rather it is bound to an EndPoint
  }

  @Override
  public void closed(EndPoint endPoint) {
    MQTTSNInterfaceManager manager = mappedInterfaces.remove(endPoint);
    if (manager != null) {
      manager.close();
    }
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    byte gatewayId;
    String gatewayConfig = endPoint.getConfig().getProperties().getProperty("gatewayId", "1");
    try {
      gatewayId = (byte) Integer.parseInt(gatewayConfig);
    } catch (Exception ex) {
      gatewayId = 1;
    }
    MQTTSNInterfaceManager manager = new MQTTSNInterfaceManager(info, endPoint, gatewayId);
    mappedInterfaces.put(endPoint, manager);
  }

  public void close() {
    for (MQTTSNInterfaceManager managers : mappedInterfaces.values()) {
      managers.close();
    }
    mappedInterfaces.clear();
  }
}
