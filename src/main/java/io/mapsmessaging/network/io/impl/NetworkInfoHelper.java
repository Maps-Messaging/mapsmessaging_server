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
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  public static int getMTU(InterfaceInformation info) throws SocketException {
    int datagramSize = info.getMTU();
    if (datagramSize != -1) {
      if (info.isLoRa()) {
        datagramSize = datagramSize - LORA_DATAGRAM_HEADER_SIZE;
      } else if (info.isIPV4()) {
        datagramSize = datagramSize - IPV4_DATAGRAM_HEADER_SIZE;
      } else {
        datagramSize = datagramSize - IPV6_DATAGRAM_HEADER_SIZE;
      }
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
