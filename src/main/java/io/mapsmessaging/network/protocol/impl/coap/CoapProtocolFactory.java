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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.NetworkInfoHelper;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoapProtocolFactory extends ProtocolImplFactory {
  private final List<CoapInterfaceManager> managers;

  public CoapProtocolFactory() {
    super("CoAP", "Constrained Application Protocol RFC7252, RFC7641, RFC7959", null);
    managers = new ArrayList<>();
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    throw new IOException("Unexpected function called");
  }

  @Override
  public String getTransportType() {
    return "udp";
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    int datagramSize = NetworkInfoHelper.getMTU(info);
    if (datagramSize > 0) {
      endPoint.getConfig().getEndPointConfig().setServerReadBufferSize(datagramSize * 2L);
      endPoint.getConfig().getEndPointConfig().setServerWriteBufferSize(datagramSize * 2L);
    }
    CoapInterfaceManager manager = new CoapInterfaceManager(endPoint, datagramSize);
    managers.add(manager);
  }

  @Override
  public void closed(EndPoint endPoint) {
    for(CoapInterfaceManager manager:managers){
      if(manager.getEndPoint().equals(endPoint)){
        managers.remove(manager);
        break;
      }
    }
  }

}
