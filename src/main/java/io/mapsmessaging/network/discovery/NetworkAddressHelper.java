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

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.network.monitor.NetworkInterfaceMonitor;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class NetworkAddressHelper {

  public List<InetAddress> getAddresses(String hostnames) {
    List<InetAddress> addressList = new ArrayList<>();
    if (hostnames != null) {
      String[] hostnameList = hostnames.split(",");
      for (String hostname : hostnameList) {
        List<InetAddress> addresses = NetworkInterfaceMonitor.getInstance().getIpAddressByName(hostname.trim());
        InetAddress primaryAddress = selectPrimaryAddress(addresses);
        if (primaryAddress != null) {
          addressList.add(primaryAddress);
        }
      }
    } else {
      List<InetAddress> addresses = NetworkInterfaceMonitor.getInstance().getCurrentIpAddresses();
      InetAddress primaryAddress = selectPrimaryAddress(addresses);
      if (primaryAddress != null) {
        addressList.add(primaryAddress);
      }
    }
    return addressList;
  }

  private InetAddress selectPrimaryAddress(List<InetAddress> addresses) {
    // Prefer IPv4 addresses first
    InetAddress primaryAddress = addresses.stream()
        .filter(address ->
            isInterfaceActive(address) &&
                !address.isLoopbackAddress() &&
                !address.isLinkLocalAddress() &&
                (address instanceof Inet4Address)
        )
        .findFirst()
        .orElse(null);

    // If no IPv4 address found, fall back to globally routable IPv6
    if (primaryAddress == null) {
      primaryAddress = addresses.stream()
          .filter(address ->
              isInterfaceActive(address) &&
                  !address.isLoopbackAddress() &&
                  !address.isLinkLocalAddress() &&
                  !isDeprecatedIPv6Address(address) &&
                  isGloballyRoutableIPv6Address(address)
          )
          .findFirst()
          .orElse(null);
    }
    return primaryAddress;
  }

  private boolean isGloballyRoutableIPv6Address(InetAddress address) {
    if (address instanceof Inet6Address) {
      Inet6Address inet6Address = (Inet6Address) address;
      return !(inet6Address.isSiteLocalAddress() || inet6Address.isLinkLocalAddress());
    }
    return false;
  }

  private boolean isInterfaceActive(InetAddress address) {
    try {
      NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
      if (networkInterface != null) {
        return networkInterface.isUp() && networkInterface.getInterfaceAddresses().stream()
            .anyMatch(ifAddr -> ifAddr.getAddress().equals(address));
      }
    } catch (SocketException e) {
      //ToDo Add Logging
    }
    return false;
  }

  private boolean isDeprecatedIPv6Address(InetAddress address) {
    if (address instanceof Inet6Address) {
      Inet6Address inet6Address = (Inet6Address) address;
      String hostAddress = inet6Address.getHostAddress();
      return hostAddress.contains("deprecated") || hostAddress.contains("temporary");
    }
    return false;
  }

}
