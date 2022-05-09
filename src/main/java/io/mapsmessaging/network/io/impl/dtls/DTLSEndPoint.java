package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;
import javax.net.ssl.SSLEngine;

public class DTLSEndPoint extends EndPoint {

  private final DTLSSessionManager manager;
  private final String clientId;
  private final SSLEngine sslEngine;

  public DTLSEndPoint(DTLSSessionManager manager, long id, String clientId, EndPointServer server, SSLEngine sslEngine) throws IOException {
    super(id, server);
    this.sslEngine = sslEngine;
    this.manager = manager;
    this.clientId = clientId;
  }

  public void close(){
    manager.close(clientId);
  }

  public String getClientId(){
    return clientId;
  }

  @Override
  public String getProtocol() {
    return "DTLS";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return manager.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return manager.getEndPoint().readPacket(packet);
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return server.getConfig().getAuthConfig();
  }

  @Override
  public String getName() {
    return "DTLS";
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(DTLSEndPoint.class.getName() + "_" + getId());
  }
}
