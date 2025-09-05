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

package io.mapsmessaging.network;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.admin.NetworkManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.ProtocolAcceptRunner;
import io.mapsmessaging.security.uuid.NamedVersions;
import io.mapsmessaging.security.uuid.UuidGenerator;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class EndPointManager implements Closeable, AcceptHandler {

  private final Logger logger;
  private final EndPointURL endPointURL;
  @Getter
  private final String protocols;
  @Getter
  private STATE state;
  @Getter
  private final UUID uniqueId;

  @Getter
  private EndPointServer endPointServer;

  public EndPointManager(EndPointURL url, EndPointServerFactory factory, EndPointServerConfigDTO endPointServerConfig, NetworkManagerJMX managerBean) throws IOException {
    logger = LoggerFactory.getLogger(NetworkManager.class.getName());
    ThreadContext.put("endpoint", url.toString());
    endPointURL = url;
    protocols = endPointServerConfig.getProtocols();
    endPointServer = null;
    state = STATE.STOPPED;
    int selectorCount = endPointServerConfig.getEndPointConfig().getSelectorThreadCount();
    EndPointManagerJMX bean = null;
    if (managerBean != null) {
      bean = new EndPointManagerJMX(managerBean.getTypePath(), this, endPointServerConfig);
    }
    SelectorLoadManager selectorLoadManager = selectorCount > 0? new SelectorLoadManager(selectorCount, url.toString()) : null;

    endPointServer = factory.instance(endPointURL,selectorLoadManager, this, endPointServerConfig, bean);
    UUID uuid;
    try {
      uuid = UuidGenerator.getInstance().generate(NamedVersions.SHA1, MessageDaemon.getInstance().getUuid(), url.toString());
    } catch (NoSuchAlgorithmException e) {
      uuid = UuidGenerator.getInstance().generate();
    }
    uniqueId = uuid;
    ThreadContext.clearAll();
  }

  public String getName() {
    return endPointURL.getJMXName();
  }

  public void start() throws IOException {
    if (state != STATE.STOPPED) {
      throw new IOException("End Point not closed, unable to start an already started End Point");
    }
    logger.log(ServerLogMessages.END_POINT_MANAGER_START, endPointURL);
    state = STATE.START;
    endPointServer.start();
    endPointServer.register();
  }

  public void close() throws IOException {
    if (state == STATE.STOPPED) {
      throw new IOException("End Point already closed");
    }
    logger.log(ServerLogMessages.END_POINT_MANAGER_CLOSE, endPointURL);
    state = STATE.STOPPED;
    endPointServer.deregister();
    endPointServer.close();
  }

  public void pause() throws IOException {
    if (state == STATE.STOPPED) {
      throw new IOException("End Point is closed, unable to pause");
    }
    if (state == STATE.PAUSED) {
      throw new IOException("End Point is already paused");
    }

    logger.log(ServerLogMessages.END_POINT_MANAGER_PAUSE, endPointURL);
    endPointServer.deregister();
    state = STATE.PAUSED;
  }

  public void resume() throws IOException {
    if (state == STATE.STOPPED) {
      throw new IOException("End Point is closed, unable to resume");
    }
    if (state != STATE.PAUSED) {
      throw new IOException("End Point is not paused");
    }
    logger.log(ServerLogMessages.END_POINT_MANAGER_RESUME, endPointURL);
    endPointServer.register();
    state = STATE.START;
  }

  @Override
  public void accept(EndPoint endpoint) throws IOException {
    ThreadContext.put("endpoint", endPointURL.toString());
    if (state == STATE.START) {
      try {
        new ProtocolAcceptRunner(endpoint, protocols);
      } catch (IOException e) {
        logger.log(ServerLogMessages.END_POINT_MANAGER_ACCEPT_EXCEPTION);
        endpoint.close();
      }
    } else {
      logger.log(ServerLogMessages.END_POINT_MANAGER_CLOSE_SERVER);
      endpoint.close();
    }
  }

  public enum STATE {
    STOPPED,
    START,
    PAUSED,
    RESUME
  }
}
