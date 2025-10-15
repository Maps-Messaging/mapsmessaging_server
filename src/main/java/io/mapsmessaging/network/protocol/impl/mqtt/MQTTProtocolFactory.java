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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.MultiByteArrayDetection;

import java.io.IOException;

public class MQTTProtocolFactory extends ProtocolImplFactory {

  private static final byte[][] PROTOCOL;

  static {
    PROTOCOL = new byte[][]{{'M', 'Q', 'T', 'T', 4}, {'M', 'Q', 'I', 's', 'd', 'p'}};
  }

  public MQTTProtocolFactory() {
    super("MQTT", "MQTT version 3.1.1 as per http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html", new MultiByteArrayDetection(PROTOCOL, 4, 2));
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    MQTTProtocol protocol = new MQTTProtocol(endPoint);
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  @Override
  public boolean matches(String protocolName){
    return super.matches(protocolName) || "mqtt-v3".equalsIgnoreCase(protocolName);
  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new MQTTProtocol(endPoint, packet);
  }

  @Override
  public String getTransportType() {
    return "tcp";
  }

}
