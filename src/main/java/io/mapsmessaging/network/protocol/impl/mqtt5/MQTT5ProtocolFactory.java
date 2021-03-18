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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.ByteArrayDetection;
import java.io.IOException;

public class MQTT5ProtocolFactory extends ProtocolImplFactory {

  private static final byte[] PROTOCOL;

  static {
    PROTOCOL = new byte[5];
    PROTOCOL[0] = 'M';
    PROTOCOL[1] = 'Q';
    PROTOCOL[2] = 'T';
    PROTOCOL[3] = 'T';
    PROTOCOL[4] = 5;
  }

  public MQTT5ProtocolFactory() {
    super("MQTT", "MQTT version 5.0 as per https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html", new ByteArrayDetection(PROTOCOL, 4));
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;

  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new MQTT5Protocol(endPoint, packet);
  }
}
