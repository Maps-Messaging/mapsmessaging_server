package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.dtls.state.StateChangeListener;
import io.mapsmessaging.network.io.impl.dtls.state.StateEngine;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DTLSEndPoint extends EndPoint implements StateChangeListener {

  private final DTLSSessionManager manager;
  private final SocketAddress clientId;
  private final StateEngine stateEngine;
  private final EndPointJMX mbean;
  private final String name;

  public DTLSEndPoint(DTLSSessionManager manager, long id, SocketAddress clientId, EndPointServer server,  StateEngine stateEngine, EndPointManagerJMX managerMBean) throws IOException {
    super(id, server);
    this.stateEngine = stateEngine;
    this.manager = manager;
    this.clientId = clientId;
    name = getProtocol() + "_" + clientId.toString();
    mbean = new EndPointJMX(managerMBean.getTypePath(), this);
    jmxParentPath = mbean.getTypePath();
    stateEngine.setListener(this);
    stateEngine.start();
  }

  public void close(){
    mbean.close();
    manager.close(clientId);
    if(server != null) {
      server.handleCloseEndPoint(this);
    }
  }

  public String getClientId(){
    return clientId.toString();
  }

  @Override
  public String getProtocol() {
    return "dtls";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return stateEngine.toNetwork(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return stateEngine.read(packet);
  }

  protected int processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return stateEngine.fromNetwork(packet);
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    if((selectionKey & SelectionKey.OP_READ) != 0) {
      stateEngine.setSelectableTask(runner);
    }
    if((selectionKey & SelectionKey.OP_WRITE) != 0) {
      stateEngine.setWriteTask(runner);
      runner.selected(runner, null, SelectionKey.OP_WRITE);
    }
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
    return name;
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(DTLSEndPoint.class.getName() + "_" + getId());
  }

  long lastAccessTime(){
    return stateEngine.getLastAccess();
  }

  @Override
  public void handshakeComplete() {
    try {
      manager.connectionComplete(this);
    } catch (IOException e) {
      close();
    }
  }
}
