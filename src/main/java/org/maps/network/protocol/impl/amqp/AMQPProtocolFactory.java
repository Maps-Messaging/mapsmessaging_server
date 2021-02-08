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

package org.maps.network.protocol.impl.amqp;

import java.io.IOException;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.detection.ByteArrayDetection;

public class AMQPProtocolFactory extends org.maps.network.protocol.ProtocolImplFactory {

  public AMQPProtocolFactory() {
    super("AMQP", "AMQP version 1.0 as per https://www.amqp.org/resources/specifications ", new ByteArrayDetection("AMQP".getBytes(), 0));
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint) throws IOException {
    return null;

  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new AMQPProtocol(endPoint, packet);
  }
}
