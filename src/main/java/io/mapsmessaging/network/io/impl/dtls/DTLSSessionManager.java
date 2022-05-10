package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.logging.ServerLogMessages;
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
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.Closeable;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DTLSSessionManager  implements Closeable, SelectorCallback {

  private static long TIMEOUT = 60000;

  private final AtomicLong uniqueId = new AtomicLong(0);
  private final Map<String, DTLSEndPoint> sessionMapping;
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
    sessionMapping = new ConcurrentHashMap<>();
    udpEndPoint.register(SelectionKey.OP_READ, selectorTask);
    SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new ReaperTask(), 30000, 30000, TimeUnit.MILLISECONDS);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    DTLSEndPoint endPoint = sessionMapping.get(packet.getFromAddress().toString());
    if(endPoint == null){
      StateEngine stateEngine;
      try {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        SSLParameters paras = sslEngine.getSSLParameters();
        int mtu = inetAddress.getMTU()-40;
        if(mtu < 0)mtu = 1460;
        paras.setMaximumPacketSize(mtu);
        sslEngine.setSSLParameters(paras);
        stateEngine = new StateEngine(packet.getFromAddress(), sslEngine, this);
      } catch (IOException e) {
        udpEndPoint.getLogger().log(ServerLogMessages.SSL_SERVER_ACCEPT_FAILED);
        return false;
      }
      endPoint = new DTLSEndPoint(this, uniqueId.incrementAndGet(), packet.getFromAddress(),  server, stateEngine, managerMBean );
      sessionMapping.put(packet.getFromAddress().toString(), endPoint);
    }

    try {
      endPoint.processPacket(packet);
    } catch (IOException e) {
      endPoint.close();
      return false;
    }
    return true;
  }

  public void close(String clientId){
    EndPoint endPoint = sessionMapping.remove(clientId);
    if(endPoint != null) {
      protocolImplFactory.closed(endPoint);
    }
  }

  @Override
  public void close()  {
    for(DTLSEndPoint endPoint:sessionMapping.values()){
      endPoint.close();
    }
    sessionMapping.clear();
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
    System.err.println("Server To:"+packet);
    return udpEndPoint.sendPacket(packet);
  }

  public class ReaperTask implements Runnable{
    public void run(){
      List<DTLSEndPoint> endPointList = new ArrayList<>(sessionMapping.values());
      long timeout = System.currentTimeMillis() - TIMEOUT;
      for(DTLSEndPoint endPoint:endPointList){
        if(endPoint.lastAccessTime() < timeout){
          endPoint.close();
        }
      }
    }
  }
}
