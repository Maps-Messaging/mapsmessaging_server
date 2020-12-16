/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt;

import java.io.IOException;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.protocol.ProtocolImplFactory;
import org.maps.network.protocol.detection.ByteArrayDetection;

public class MQTTProtocolFactory extends ProtocolImplFactory {

  private static final byte[] PROTOCOL;

  static {
    PROTOCOL = new byte[]{'M', 'Q', 'T', 'T', 4};
  }

  public MQTTProtocolFactory() {
    super("MQTT", "MQTT version 3.1.1 as per http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html", new ByteArrayDetection(PROTOCOL, 4));
  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new MQTTProtocol(endPoint, packet);
  }
}
