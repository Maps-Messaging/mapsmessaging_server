package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.dtls.state.StateEngine;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.impl.udp.UDPInterfaceInformation;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionManager;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DTLSSessionManager  implements Closeable, SelectorCallback {

  private static final long TIMEOUT = 600;

  private final AtomicLong uniqueId = new AtomicLong(0);
  private final UDPSessionManager<DTLSEndPoint> sessionMapping;
  private final UDPEndPoint udpEndPoint;
  private final SelectorTask selectorTask;
  private final EndPointServer server;
  private final ProtocolImplFactory protocolImplFactory;
  private final SSLContext sslContext;
  private final AcceptHandler acceptHandler;
  private final UDPInterfaceInformation inetAddress;
  private final EndPointManagerJMX managerMBean;

  public DTLSSessionManager(UDPEndPoint udpEndPoint,
      NetworkInterface inetAddress,
      EndPointServer server,
      ProtocolImplFactory protocolImplFactory,
      SSLContext sslContext,
      AcceptHandler acceptHandler,
      EndPointManagerJMX managerMBean)
      throws IOException {
    this.udpEndPoint = udpEndPoint;
    this.sslContext = sslContext;
    this.server = server;
    this.acceptHandler = acceptHandler;
    this.protocolImplFactory = protocolImplFactory;
    this.inetAddress = new UDPInterfaceInformation(inetAddress);
    this.managerMBean = managerMBean;
    selectorTask = new SelectorTask(this, udpEndPoint.getConfig().getProperties(), udpEndPoint.isUDP());
    sessionMapping = new UDPSessionManager<>(TIMEOUT);
    udpEndPoint.register(SelectionKey.OP_READ, selectorTask);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    UDPSessionState<DTLSEndPoint> state = sessionMapping.getState(packet.getFromAddress());
    DTLSEndPoint endPoint;
    if(state == null){
      StateEngine stateEngine;
      SSLEngine sslEngine = sslContext.createSSLEngine();
      SSLParameters paras = sslEngine.getSSLParameters();
      //int mtu = inetAddress.getMTU()-40;
      int mtu = 1460;
      paras.setMaximumPacketSize(mtu);
      sslEngine.setSSLParameters(paras);
      stateEngine = new StateEngine(packet.getFromAddress(), sslEngine, this);
      endPoint = new DTLSEndPoint(this, uniqueId.incrementAndGet(), packet.getFromAddress(),  server, stateEngine, managerMBean );
      sessionMapping.addState(packet.getFromAddress(), new UDPSessionState<>(endPoint));
    }
    else{
      endPoint = state.getContext();
    }

    try {
      endPoint.processPacket(packet);
    } catch (IOException e) {
      endPoint.close();
      return false;
    }
    return true;
  }

  public void close(SocketAddress clientId){
    UDPSessionState<DTLSEndPoint> state = sessionMapping.getState(clientId);
    if(state != null && state.getContext() != null) {
      protocolImplFactory.closed(state.getContext());
    }
    selectorTask.close();
  }

  @Override
  public void close()  {
    sessionMapping.close();
    udpEndPoint.close();
  }

  public void connectionComplete(DTLSEndPoint endPoint) throws IOException {
    acceptHandler.accept(endPoint);
    protocolImplFactory.create(endPoint, inetAddress);
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
    sessionMapping.getState(packet.getFromAddress());
    return udpEndPoint.sendPacket(packet);
  }
}
