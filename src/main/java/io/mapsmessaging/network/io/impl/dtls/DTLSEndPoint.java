/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.dtls.state.StateChangeListener;
import io.mapsmessaging.network.io.impl.dtls.state.StateEngine;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;

public class DTLSEndPoint extends EndPoint implements StateChangeListener, Timeoutable {

  private final DTLSSessionManager manager;
  private final SocketAddress clientId;
  private final StateEngine stateEngine;
  private final EndPointJMX mbean;

  public DTLSEndPoint(DTLSSessionManager manager, long id, SocketAddress clientId, EndPointServer server, StateEngine stateEngine, EndPointManagerJMX managerMBean)
      throws IOException {
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

  @Override
  public void close() {
    mbean.close();
    manager.close(clientId);
    if (server != null) {
      server.handleCloseEndPoint(this);
    }
  }

  public String getClientId() {
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
    if ((selectionKey & SelectionKey.OP_READ) != 0) {
      stateEngine.setSelectableTask(runner);
    }
    if ((selectionKey & SelectionKey.OP_WRITE) != 0) {
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
    return server.getConfig().getAuthenticationRealm();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(DTLSEndPoint.class.getName() + "_" + getId());
  }

  @Override
  public void handshakeComplete() {
    try {
      manager.connectionComplete(this);
    } catch (IOException e) {
      close();
    }
  }

  @Override
  public boolean isUDP() {
    return true;
  }

}
