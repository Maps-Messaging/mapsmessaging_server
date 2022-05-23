package io.mapsmessaging.network.protocol.impl.forwarder;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ForwarderProtocolFactory extends ProtocolImplFactory {

  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;
  private final Map<EndPoint, ForwarderProtocol> mappedInterfaces;

  public ForwarderProtocolFactory() {
    super("forwarder", "Packet Forwarder",null);
    mappedInterfaces = new ConcurrentHashMap<>();
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) {
    // This protocol is not constructed by a packet, rather it is bound to an EndPoint
  }

  @Override
  public void closed(EndPoint endPoint) {
  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    int datagramSize = info.getMTU();
    if (datagramSize != -1) {
      if(info.isLoRa()){
        datagramSize = datagramSize - LORA_DATAGRAM_HEADER_SIZE;
      }
      else if (info.isIPV4()) {
        datagramSize = datagramSize - IPV4_DATAGRAM_HEADER_SIZE;
      } else {
        datagramSize = datagramSize - IPV6_DATAGRAM_HEADER_SIZE;
      }
      endPoint.getConfig().getProperties().put("serverReadBufferSize", "" + datagramSize);
      endPoint.getConfig().getProperties().put("serverWriteBufferSize", "" + datagramSize);
    }

    mappedInterfaces.put(endPoint, new ForwarderProtocol(endPoint));
  }

  public void close() {

  }
}
