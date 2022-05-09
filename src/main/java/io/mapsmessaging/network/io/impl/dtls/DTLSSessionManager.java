package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DTLSSessionManager  implements Closeable, SelectorCallback {

  private final AtomicLong uniqueId = new AtomicLong(0);
  private final Map<String, DTLSEndPoint> sessionMapping;
  private final UDPEndPoint udpEndPoint;
  private final SelectorTask selectorTask;
  private final EndPointServer server;
  private final ProtocolImplFactory protocolImplFactory;
  private final SSLContext sslContext;
  private final AcceptHandler acceptHandler;

  public DTLSSessionManager(UDPEndPoint udpEndPoint, EndPointServer server, ProtocolImplFactory protocolImplFactory, SSLContext sslContext, AcceptHandler acceptHandler)
      throws IOException {
    this.udpEndPoint = udpEndPoint;
    this.sslContext = sslContext;
    this.server = server;
    this.acceptHandler = acceptHandler;
    this.protocolImplFactory = protocolImplFactory;
    selectorTask = new SelectorTask(this, udpEndPoint.getConfig().getProperties(), udpEndPoint.isUDP());
    sessionMapping = new ConcurrentHashMap<>();
    udpEndPoint.register(SelectionKey.OP_READ, selectorTask);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    System.err.println("Received::"+packet);
    DTLSEndPoint endPoint = sessionMapping.get(packet.getFromAddress().toString());
    if(endPoint == null){
      try {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        SSLParameters paras = sslEngine.getSSLParameters();
        paras.setMaximumPacketSize(1024);
        sslEngine.setSSLParameters(paras);
        endPoint = new DTLSEndPoint(this, uniqueId.incrementAndGet(), packet.getFromAddress(),  server, sslEngine);
        acceptHandler.accept(endPoint);
      } catch (IOException e) {
        udpEndPoint.getLogger().log(ServerLogMessages.SSL_SERVER_ACCEPT_FAILED);
        return false;
      }
      sessionMapping.put(packet.getFromAddress().toString(), endPoint);
    }

    endPoint.readPacket(packet);
    return true;
  }

  public void close(String clientId){
    sessionMapping.remove(clientId);
  }

  @Override
  public void close()  {
    udpEndPoint.close();
  }

  @Override
  public String getName() {
    return "DTLS-Session Management";
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

  public int sendPacket(Packet packet) throws IOException {
    System.err.println("Sending ::"+packet);
    int val = udpEndPoint.sendPacket(packet);
    packet.clear();
    return val;
  }
}
