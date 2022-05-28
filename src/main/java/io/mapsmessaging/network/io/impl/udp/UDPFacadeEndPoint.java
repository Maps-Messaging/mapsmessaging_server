package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;

public class UDPFacadeEndPoint extends EndPoint {

  private final EndPoint endPoint;
  private final SocketAddress fromAddress;

  public UDPFacadeEndPoint(EndPoint endPoint, SocketAddress fromAddress, EndPointServerStatus server) {
    super(1, server);
    this.endPoint = endPoint;
    this.fromAddress = fromAddress;
    List<String> end = endPoint.getJMXTypePath();
    end.remove(end.size()-1);
    String entry = "endPointName="+endPoint.getName()+"_"+fromAddress.toString();
    while(entry.contains(":")){
      entry = entry.replace(":", "_");
    }
    end.add(entry);
    jmxParentPath = end;
  }

  @Override
  public String getProtocol() {
    return endPoint.getProtocol();
  }

  public List<String> getJMXTypePath() {
    return jmxParentPath;
  }


  @Override
  public int sendPacket(Packet packet) throws IOException {
    return endPoint.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return endPoint.readPacket(packet);
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return endPoint.register(selectionKey, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return endPoint.deregister(selectionKey);
  }

  @Override
  public String getAuthenticationConfig() {
    return endPoint.getAuthenticationConfig();
  }

  @Override
  public String getName() {
    return fromAddress.toString();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(UDPFacadeEndPoint.class);
  }
}
