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

package io.mapsmessaging.rest.jolokia;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.JolokiaConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class JolokaManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(JolokaManager.class);

  private final JolokiaConfig config;
  private final boolean enabled;
  private Startup startup;
  private String errorMsg;

  public JolokaManager() {
    config = JolokiaConfig.getInstance();
    enabled = config.isEnable();
    errorMsg = "";
  }

  @Override
  public String getName() {
    return "Jolokia Manager";
  }

  @Override
  public String getDescription() {
    return "JMX RestAPI mapper";
  }

  public void start() {
    if (enabled) {
      startup = new Startup(config);
      Thread runner = new Thread(startup);
      runner.setDaemon(true);
      runner.start();
    }
  }

  public void stop() {
    if (startup != null) {
      startup.stop();
    }
  }

  private class Startup implements Runnable {

    private JolokiaServer jolokiaServer;
    private final JolokiaConfig config;

    public Startup(JolokiaConfig config){
      this.config = config;
    }

    public void stop() {
      if (jolokiaServer != null) {
        try {
          jolokiaServer.stop();
          jolokiaServer = null;
        } catch (Exception e) {
          // Do not log this, Jolokia throws a NPE every time we shut down
        }
      }
    }

    public void run() {
      HashMap<String, String> map = new HashMap<>();
      ConfigurationProperties properties = config.getJolokiaMapping();
      for (Entry<String, Object> entry : properties.entrySet()) {
        String val = entry.getValue().toString();
        if(val.endsWith(".0")){
          val = val.substring(0, val.indexOf("."));
        }
        map.put(entry.getKey(), val);
      }
      map.put("agentId", MessageDaemon.getInstance().getId());
      map.put("agentDescription", "Maps Messaging Server");
      map.put("logHandlerClass", JolokiaLogHandler.class.getName());
      JolokiaServerConfig jolokiaConfig = new JolokiaServerConfig(map);
      try {
        jolokiaServer = new JolokiaServer(jolokiaConfig, true);
        jolokiaServer.start();
      } catch (IOException e) {
        errorMsg = e.getMessage();
        logger.log(ServerLogMessages.JOLOKIA_STARTUP_FAILURE, e);
      }
    }
  }
  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if (enabled) {
      status.setStatus(Status.OK);
      if(startup != null) {
        status.setStatus(Status.STOPPED);
      }
      else{
        if(!errorMsg.isEmpty()){
          status.setStatus(Status.ERROR);
          status.setComment(errorMsg);
        }
      }
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

}
