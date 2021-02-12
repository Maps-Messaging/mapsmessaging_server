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

package org.maps.network.protocol.impl.mqtt_sn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.maps.network.io.EndPoint;
import org.maps.network.io.InterfaceInformation;
import org.maps.network.io.Packet;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.ProtocolImplFactory;

// The protocol is MQTT_SN so it makes sense
@java.lang.SuppressWarnings("squid:S00101")
public class MQTT_SNProtocolFactory extends ProtocolImplFactory {


  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  private final List<MQTTSNInterfaceManager> mappedInterfaces;

  public MQTT_SNProtocolFactory() {
    super("MQTT-SN", "MQTT-SN UDP based protocol as per http://mqtt.org/mqtt-specification/",null);
    mappedInterfaces = new ArrayList<>();
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) {
    // This protocol is not constructed by a packet, rather it is bound to an EndPoint
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    int datagramSize = info.getMTU();
    if (datagramSize != -1) {
      if(info.isLoRa()){
        datagramSize = datagramSize - LORA_DATAGRAM_HEADER_SIZE;
      }
      else if (info.isIPV4()) {
        datagramSize = datagramSize - IPV4_DATAGRAM_HEADER_SIZE;
      } else {
        datagramSize = datagramSize - IPV6_DATAGRAM_HEADER_SIZE;
      }
      endPoint.getConfig().getProperties().put("serverReadBufferSize", "" + datagramSize);
      endPoint.getConfig().getProperties().put("serverWriteBufferSize", "" + datagramSize);
    }
    byte gatewayId;
    String gatewayConfig = endPoint.getConfig().getProperties().getProperty("gatewayId", "1");
    try {
      gatewayId = (byte) Integer.parseInt(gatewayConfig);
    } catch (Exception ex) {
      gatewayId = 1;
    }

    MQTTSNInterfaceManager manager = new MQTTSNInterfaceManager(info, endPoint, gatewayId);
    mappedInterfaces.add(manager);
  }

  public void close() {
    for (MQTTSNInterfaceManager managers : mappedInterfaces) {
      managers.close();
    }
    mappedInterfaces.clear();
  }
}
