package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.NetworkInfoHelper;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoapProtocolFactory extends ProtocolImplFactory {

  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  private final List<CoapInterfaceManager> managers;

  public CoapProtocolFactory() {
    super("CoAP", "Constrained Application Protocol RFC7252, RFC7641, RFC7959", null);
    managers = new ArrayList<>();
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
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
      endPoint.getConfig().getProperties().put("serverReadBufferSize", "" + datagramSize * 2);
      endPoint.getConfig().getProperties().put("serverWriteBufferSize", "" + datagramSize * 2);
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
