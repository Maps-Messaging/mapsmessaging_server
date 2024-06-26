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
