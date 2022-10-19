/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;

public class JolokaManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(JolokaManager.class);

  private final ConfigurationProperties properties;
  private final boolean enabled;
  private Startup startup;

  public JolokaManager() {
    properties = ConfigurationManager.getInstance().getProperties("jolokia");
    enabled = properties.getProperty("enable", "true").equalsIgnoreCase("true");
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
      startup = new Startup();
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
      for (Entry<String, Object> entry : properties.entrySet()) {
        map.put(entry.getKey(), entry.getValue().toString());
      }
      JolokiaServerConfig config = new JolokiaServerConfig(map);
      try {
        jolokiaServer = new JolokiaServer(config, true);
        jolokiaServer.start();
      } catch (IOException e) {
        logger.log(ServerLogMessages.JOLOKIA_STARTUP_FAILURE, e);
      }
    }
  }
}
