package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.SessionManager;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class UDPEndPointManager implements SelectorCallback, SessionManager {
  // https://en.wikipedia.org/wiki/User_Datagram_Protocol
  private static final int IPV4_DATAGRAM_HEADER_SIZE = 20;
  private static final int IPV6_DATAGRAM_HEADER_SIZE = 40;
  private static final int LORA_DATAGRAM_HEADER_SIZE = 4;

  private final UDPEndPoint udpEndPoint;
  private final Map<String, ProtocolImpl> sessionMapping;
  private final ProtocolImplFactory protocolFactory;
  private final SelectorTask selectorTask;
  private final UDPInterfaceInformation interfaceInformation;

  public UDPEndPointManager(UDPEndPoint endPoint, ProtocolImplFactory protocolFactory, UDPInterfaceInformation nInfo) throws IOException {
    udpEndPoint = endPoint;
    interfaceInformation = nInfo;
    sessionMapping = new ConcurrentHashMap<>();
    this.protocolFactory = protocolFactory;
    protocolFactory.registerSessionManager(this);
    int datagramSize = interfaceInformation.getMTU();
    if (datagramSize != -1) {
      if(interfaceInformation.isLoRa()){
        datagramSize = datagramSize - LORA_DATAGRAM_HEADER_SIZE;
      }
      else if (interfaceInformation.isIPV4()) {
        datagramSize = datagramSize - IPV4_DATAGRAM_HEADER_SIZE;
      } else {
        datagramSize = datagramSize - IPV6_DATAGRAM_HEADER_SIZE;
      }
      udpEndPoint.getConfig().getProperties().put("serverReadBufferSize", "" + datagramSize);
      udpEndPoint.getConfig().getProperties().put("serverWriteBufferSize", "" + datagramSize);
    }
    selectorTask = new SelectorTask(this, udpEndPoint.getConfig().getProperties(), udpEndPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    protocolFactory.create(endPoint, interfaceInformation);
  }

  @Override
  public void close() throws IOException {
    selectorTask.cancel(SelectionKey.OP_READ);
    selectorTask.close();
    udpEndPoint.close();
    sessionMapping.clear();
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    ProtocolImpl protocol = sessionMapping.get(packet.getFromAddress().toString());
    if(protocol != null){
      protocol.processPacket(packet);
    }
    else{
      protocolFactory.detect(packet);
    }
    return true;
  }


  public void openSession(ProtocolImpl protocol, SocketAddress socketAddress){
    sessionMapping.put(socketAddress.toString(), protocol);
  }

  public void closeSession(SocketAddress socketAddress){
    sessionMapping.remove(socketAddress.toString());
  }

  @Override
  public String getName() {
    return "UDP Manager";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return udpEndPoint;
  }
}
