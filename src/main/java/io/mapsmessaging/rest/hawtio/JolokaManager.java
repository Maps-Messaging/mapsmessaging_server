/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.hawtio;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.JolokiaConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;
import org.jolokia.util.LogHandler;

public class JolokaManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(JolokaManager.class);

  private final JolokiaConfig config;
  private final boolean enabled;
  private Startup startup;

  public JolokaManager() {
    config = new JolokiaConfig(ConfigurationManager.getInstance().getProperties("jolokia"));
    enabled = config.isEnable();
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
        } catch (Exception e) {
          // Do not log this, Jolokia throws a NPE every time we shut down
        }
      }
    }

    public void run() {
      HashMap<String, String> map = new HashMap<>();
      ConfigurationProperties properties = config.getJolokiaMapping();
      for (Entry<String, Object> entry : properties.entrySet()) {
        map.put(entry.getKey(), entry.getValue().toString());
      }
      map.put("agentId", MessageDaemon.getInstance().getId());
      map.put("agentDescription", "Maps Messaging Server");
      map.put("logHandlerClass", JolokiaLogHandler.class.getName());
      JolokiaServerConfig jolokiaConfig = new JolokiaServerConfig(map);
      try {
        jolokiaServer = new JolokiaServer(jolokiaConfig, true);
        jolokiaServer.start();
      } catch (IOException e) {
        logger.log(ServerLogMessages.JOLOKIA_STARTUP_FAILURE, e);
      }
      LogHandler.StdoutLogHandler logg;
    }
  }

}
