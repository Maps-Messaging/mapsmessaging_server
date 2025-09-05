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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.impl.udp.UDPInterfaceInformation;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkInfoHelper {

  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 28;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 48;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  public static int getMTU(InterfaceInformation info) throws SocketException {
    int datagramSize = info.getMTU();
    if(datagramSize < 0){
      datagramSize = 1500;
    }
    if (info.isLoRa()) {
      datagramSize = datagramSize - LORA_DATAGRAM_HEADER_SIZE;
    } else if (info.isIPV4()) {
      datagramSize = datagramSize - IPV4_DATAGRAM_HEADER_SIZE;
    } else {
      datagramSize = datagramSize - IPV6_DATAGRAM_HEADER_SIZE;
    }
    return datagramSize;
  }


  public static List<UDPInterfaceInformation> createInterfaceList() throws SocketException {
    List<UDPInterfaceInformation> result = new ArrayList<>();
    Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
    while (enumeration.hasMoreElements()) {
      NetworkInterface networkInterface = enumeration.nextElement();
      if (!networkInterface.getInterfaceAddresses().isEmpty()) {
        result.add(new UDPInterfaceInformation(networkInterface));
      }
    }
    return result;
  }

  private NetworkInfoHelper() {
  }
}
