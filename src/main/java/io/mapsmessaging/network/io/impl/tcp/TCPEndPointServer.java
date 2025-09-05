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

package io.mapsmessaging.network.io.impl.tcp;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TCPEndPointServer extends EndPointServer {

  protected final EndPointManagerJMX managerMBean;
  protected final SelectorLoadManager selectorLoadManager;
  protected final Selector selector;
  protected final String authenticationConfig;
  private final InetSocketAddress bindAddress;
  private final int backLog;
  private final int selectorTaskWait;
  protected ServerSocketChannel serverSocket;
  private SelectionKey selectionKey;
  private SelectableChannel selectable;

  public TCPEndPointServer(InetSocketAddress bindAddr, SelectorLoadManager sel, AcceptHandler accept, EndPointServerConfigDTO config, EndPointURL url, EndPointManagerJMX managerMBean) {
    super(accept, url, config);
    this.managerMBean = managerMBean;
    selectorLoadManager = sel;
    selector = selectorLoadManager.allocate(); // Used for accept
    authenticationConfig = config.getAuthenticationRealm();
    bindAddress = bindAddr;
    backLog = config.getBacklog();
    selectorTaskWait = config.getSelectorTaskWait();
  }

  // We need to open a socket, its a socket library so we can ignore this issue
  @java.lang.SuppressWarnings("squid:S4818")
  public void start() throws IOException {
    serverSocket = ServerSocketChannel.open();
    serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    serverSocket.bind(bindAddress, backLog);
    selectable = serverSocket.configureBlocking(false);
    logger.log(ServerLogMessages.TCP_SERVER_ENDPOINT_CREATE, bindAddress.getPort(), backLog, bindAddress.getHostName());
  }

  @Override
  public void close() throws IOException {
    super.close();
    logger.log(ServerLogMessages.TCP_SERVER_ENDPOINT_CLOSE);
    deregister();
    serverSocket.close();
  }

  public void register() throws IOException {
    logger.log(ServerLogMessages.TCP_SERVER_ENDPOINT_REGISTER);
    FutureTask<SelectionKey> task = selector.register(selectable, SelectionKey.OP_ACCEPT, this);
    try {
      selectionKey = task.get(selectorTaskWait, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e.getMessage());
    } catch (ExecutionException e) {
      throw new IOException("Future task failed", e.getCause());
    } catch (TimeoutException e) {
      throw new IOException("Selector Thread task exceeded timeout");
    }
  }

  public void deregister() {
    logger.log(ServerLogMessages.TCP_SERVER_ENDPOINT_DEREGISTER);
    if (selectionKey != null) {
      selectionKey.cancel();
    }
    selector.wakeup();
  }

  public void selected(Selectable selectable, Selector sel, int selection) {
    try {
      handleNewEndPoint(new TCPEndPoint(
          generateID(),
          serverSocket.accept().socket(),
          selectorLoadManager.allocate(),
          authenticationConfig,
          this,
          managerMBean));
    } catch (IOException e) {
      logger.log(ServerLogMessages.TCP_SERVER_ENDPOINT_ACCEPT);
    }
  }

  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(TCPEndPointServer.class.getName() + "_" + url);
  }
}
