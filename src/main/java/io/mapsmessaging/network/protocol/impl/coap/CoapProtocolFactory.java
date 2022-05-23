package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoapProtocolFactory extends ProtocolImplFactory {

  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  private final Map<EndPoint, CoapInterfaceManager> mappedInterfaces;

  public CoapProtocolFactory(){
    super("CoAP", "Constrained Application Protocol RFC7252", null);
    mappedInterfaces = new ConcurrentHashMap<>();
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {

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
    CoapInterfaceManager manager = new CoapInterfaceManager(info, endPoint);
    mappedInterfaces.put(endPoint, manager);
  }
}
