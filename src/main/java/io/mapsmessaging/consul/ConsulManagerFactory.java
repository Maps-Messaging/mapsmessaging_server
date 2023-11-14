/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.consul;

import io.mapsmessaging.consul.ecwid.EcwidConsulManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.rest.RestApiServerManager;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.locks.LockSupport;

public class ConsulManagerFactory {

  private static final ConsulManagerFactory instance;

  static {
    instance = new ConsulManagerFactory();
  }

  public static ConsulManagerFactory getInstance() {
    return instance;
  }

  private final Logger logger = LoggerFactory.getLogger(ConsulManagerFactory.class);
  private final boolean forceWait;
  private ConsulServerApi manager;

  public synchronized void start(String serverId) {
    stop(); // just to be sure
    logger.log(ServerLogMessages.CONSUL_MANAGER_START, serverId);
    boolean retry = true;
    int counter = 0;
    while (retry && counter < Constants.RETRY_COUNT) {
      try {
        manager = new EcwidConsulManager(serverId);
        retry = false;
      }
      catch(IOException io){
        logger.log(ServerLogMessages.CONSUL_MANAGER_START_ABORTED, serverId, io);
        retry = false;
      } catch (Exception e) {
        LockSupport.parkNanos(1000000000L);
        counter++;
        if (!forceWait && e instanceof IOException) {
          Exception actual = (Exception) e.getCause();
          if (actual instanceof ConnectException) {
            logger.log(ServerLogMessages.CONSUL_MANAGER_START_SERVER_NOT_FOUND, serverId);
          } else {
            logger.log(ServerLogMessages.CONSUL_MANAGER_START_ABORTED, serverId, e);
          }
          return;
        }
        logger.log(ServerLogMessages.CONSUL_MANAGER_START_DELAYED, serverId);
      }
    }
  }

  public String getPath(){
    if(manager != null){
      return manager.getUrlPath();
    }
    return null;
  }

  public void register(RestApiServerManager restApiServerManager){
    if(manager != null){
      manager.register(restApiServerManager);
    }
  }

  public void register(EndPointServer endPointServer){
    if(manager != null && endPointServer.getConfig().getProperties().getBooleanProperty("discoverable", false)){
      manager.register(endPointServer);
    }
  }

  public synchronized void stop() {
    if (manager != null) {
      logger.log(ServerLogMessages.CONSUL_MANAGER_STOP);
      manager.stop();
    }
  }

  public synchronized ConsulServerApi getManager() {
    return manager;
  }

  public synchronized boolean isStarted() {
    return manager != null;
  }

  private ConsulManagerFactory() {
    boolean config;
    try {
      config = Boolean.parseBoolean(System.getProperty("ForceConsul", "FALSE"));
    } catch (Exception e) {
      config = false;
    }
    forceWait = config;
    manager = null;
  }
}
