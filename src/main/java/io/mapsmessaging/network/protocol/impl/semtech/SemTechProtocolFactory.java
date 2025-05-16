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

package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;

import java.io.IOException;

public class SemTechProtocolFactory extends ProtocolImplFactory {

//  private final Map<EndPoint, SemTechProtocol> mappedInterfaces;

  public SemTechProtocolFactory() {
    super("semtech", "SemTech UDP protocol", null);
  //  mappedInterfaces = new ConcurrentHashMap<>();
  }

  @Override
  public void closed(EndPoint endPoint) {
  //  mappedInterfaces.remove(endPoint);
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    SemTechProtocol protocol = new SemTechProtocol(endPoint, sessionId);
 //   mappedInterfaces.put(endPoint, protocol);
    return protocol;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    // No Op since this is a UDP transport
  }

  @Override
  public String getTransportType() {
    return "udp";
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    new SemTechProtocol(endPoint);
   // mappedInterfaces.put(endPoint, protocol);
  }
}
