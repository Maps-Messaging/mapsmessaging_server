/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network;

import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.admin.NetworkManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.ProtocolAcceptRunner;
import java.io.Closeable;
import java.io.IOException;
import org.apache.logging.log4j.ThreadContext;

public class EndPointManager implements Closeable, AcceptHandler {

  private final Logger logger;
  private final EndPointURL endPointURL;
  private final String protocols;
  private STATE state;
  private EndPointServer endPointServer;

  public EndPointManager(EndPointURL url, EndPointServerFactory factory, NetworkConfig nc, NetworkManagerJMX managerBean) throws IOException {
    logger = LoggerFactory.getLogger(NetworkManager.class.getName());
    ThreadContext.put("endpoint", url.toString());
    endPointURL = url;
    protocols = nc.getProtocols();
    endPointServer = null;
    state = STATE.CLOSED;
    int selectorCount = nc.getProperties().getIntProperty("selectorThreadCount", 5);
    EndPointManagerJMX bean = new EndPointManagerJMX(managerBean.getTypePath(), this, nc);
    endPointServer = factory.instance(endPointURL, new SelectorLoadManager(selectorCount), this, nc, bean);
    ThreadContext.clearAll();
  }

  public STATE getState() {
    return state;
  }

  public String getProtocols() {
    return protocols;
  }

  public String getName() {
    return endPointURL.getJMXName();
  }

  public void start() throws IOException {
    if (state != STATE.CLOSED) {
      throw new IOException("End Point not closed, unable to open an already open End Point");
    }
    logger.log(LogMessages.END_POINT_MANAGER_START, endPointURL);
    state = STATE.OPEN;
    endPointServer.start();
    endPointServer.register();
  }

  public void close() throws IOException {
    if (state == STATE.CLOSED) {
      throw new IOException("End Point already closed");
    }
    logger.log(LogMessages.END_POINT_MANAGER_CLOSE, endPointURL);
    state = STATE.CLOSED;
    endPointServer.deregister();
    endPointServer.close();
  }

  public void pause() throws IOException {
    if (state == STATE.CLOSED) {
      throw new IOException("End Point is closed, unable to pause");
    }
    if (state == STATE.PAUSED) {
      throw new IOException("End Point is already paused");
    }

    logger.log(LogMessages.END_POINT_MANAGER_PAUSE, endPointURL);
    endPointServer.deregister();
    state = STATE.PAUSED;
  }

  public void resume() throws IOException {
    if (state == STATE.CLOSED) {
      throw new IOException("End Point is closed, unable to resume");
    }
    if (state != STATE.PAUSED) {
      throw new IOException("End Point is not paused");
    }
    logger.log(LogMessages.END_POINT_MANAGER_RESUME, endPointURL);
    endPointServer.register();
    state = STATE.OPEN;
  }

  @Override
  public void accept(EndPoint endpoint) throws IOException {
    ThreadContext.put("endpoint", endPointURL.toString());
    if (state == STATE.OPEN) {
      try {
        new ProtocolAcceptRunner(endpoint, protocols);
      } catch (IOException e) {
        logger.log(LogMessages.END_POINT_MANAGER_ACCEPT_EXCEPTION);
        endpoint.close();
      }
    } else {
      logger.log(LogMessages.END_POINT_MANAGER_CLOSE_SERVER);
      endpoint.close();
    }
  }

  public EndPointServer getEndPointServer() {
    return endPointServer;
  }

  enum STATE {
    CLOSED,
    OPEN,
    PAUSED
  }
}
