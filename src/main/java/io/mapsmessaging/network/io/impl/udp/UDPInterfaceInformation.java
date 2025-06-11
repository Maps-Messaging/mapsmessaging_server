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

package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.network.io.InterfaceInformation;

import java.net.*;
import java.util.List;

public class UDPInterfaceInformation implements InterfaceInformation {

  private final NetworkInterface netint;
  private final InetAddress bcast;

  public UDPInterfaceInformation(NetworkInterface inetAddress) {
    netint = inetAddress;
    bcast = null;
  }

  public UDPInterfaceInformation(UDPInterfaceInformation info, InetAddress broadcast) {
    this.netint = info.netint;
    if(broadcast != null && !broadcast.isAnyLocalAddress()) {
      bcast = broadcast;
    }
    else{
      bcast = null;
    }
  }

  public boolean isLoopback() throws SocketException {
    return netint.isLoopback();
  }

  public boolean isUp() throws SocketException {
    return netint.isUp();
  }

  public boolean isLoRa() {
    return false;
  }

  public List<InterfaceAddress> getInterfaces() {
    return netint.getInterfaceAddresses();
  }

  public int getMTU() throws SocketException {
    return netint.getMTU();
  }

  public String toString() {
    return netint.toString();
  }

  public boolean isIPV4() {
    for (InterfaceAddress inetAddress : getInterfaces()) {
      if (inetAddress.getAddress() instanceof Inet6Address) {
        return false;
      }
    }
    return true;
  }

  @Override
  public InetAddress getBroadcast() {
    return bcast;
  }
}
