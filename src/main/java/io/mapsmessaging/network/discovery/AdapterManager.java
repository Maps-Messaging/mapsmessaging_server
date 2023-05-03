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

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import lombok.Getter;

public class AdapterManager {

  private final Logger logger;
  private final String serverName;
  private final JmDNS mDNSAgent;
  private final Map<EndPointServer, List<ServiceInfo>> endPointList;
  private final boolean stampMeta;

  @Getter
  private final String adapter;

  public AdapterManager(String adapter, String serverName, JmDNS agent, boolean stampMeta) {
    this.serverName = serverName;
    this.adapter = adapter;
    this.mDNSAgent = agent;
    this.stampMeta = stampMeta;
    logger = LoggerFactory.getLogger(AdapterManager.class);
    endPointList = new LinkedHashMap<>();
  }

  public synchronized void register(EndPointServer endPointServer, String transport, List<String> protocolList){
    EndPointURL url = endPointServer.getUrl();
    List<ServiceInfo> serviceInfoList = new ArrayList<>();
    for (String protocol : protocolList) {
      String lowerProtocol = protocol.toLowerCase();
      Map<String, String> map = new LinkedHashMap<>();
      if(stampMeta) {
        switch (lowerProtocol) {
          case "mqtt":
            map.put("version 3.1", "true");
            map.put("version 3.1.1", "true");
            map.put("version 5.0", "true");
            break;
          case "amqp":
            map.put("version 1.0", "true");
            break;
          case "stomp":
            map.put("version 1.2", "true");
            break;

          default:
        }
        map.put("schema support", "true");
        map.put("schema name", "$schema");
        map.put("version", BuildInfo.getBuildVersion());
        map.put("server name", MessageDaemon.getInstance().getId());
        map.put("date", BuildInfo.getBuildDate());
      }
      String service = "_" + lowerProtocol + "._"+transport+"._local";
      ServiceInfo serviceInfo = ServiceInfo.create(service, serverName, url.getPort(), 0, 0, map);
      serviceInfoList.add(serviceInfo);
    }
    endPointList.put(endPointServer, serviceInfoList);
    Runnable r = () -> {
      for (ServiceInfo serviceInfo : serviceInfoList) {
        try {
          register(serviceInfo);
        } catch (IOException e) {
          logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_REGISTER, e);
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
  }

  public synchronized void register(ServiceInfo serviceInfo) throws IOException {
    mDNSAgent.registerService(serviceInfo);
    logger.log(ServerLogMessages.DISCOVERY_REGISTERED_SERVICE, serviceInfo);
  }

  public synchronized void deregister(EndPointServer endPointServer) {
    List<ServiceInfo> list = endPointList.remove(endPointServer);
    if (list != null) {
      for (ServiceInfo info : list) {
        deregister(info);
      }
    }
  }

  public synchronized void deregister(ServiceInfo serviceInfo) {
    mDNSAgent.unregisterService(serviceInfo);
    logger.log(ServerLogMessages.DISCOVERY_DEREGISTERED_SERVICE, serviceInfo);
  }

  public synchronized void deregisterAll() {
    logger.log(ServerLogMessages.DISCOVERY_DEREGISTERED_ALL);
    mDNSAgent.unregisterAllServices();
    endPointList.clear();
  }

  public void registerListener(String type, ServiceListener listener) {
    mDNSAgent.addServiceListener(type, listener);
  }

  public void removeListener(String type, ServiceListener listener) {
    mDNSAgent.removeServiceListener(type, listener);
  }

}
