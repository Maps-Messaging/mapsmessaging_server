/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.configuration.ConfigurationProperties;
import org.maps.utilities.configuration.PropertyManager;

public class JolokaManager {
  private final Logger logger = LoggerFactory.getLogger(JolokaManager.class);

  private final ConfigurationProperties properties;
  private final boolean enabled;
  private Startup startup;

  public JolokaManager() {
    properties = PropertyManager.getInstance().getProperties("jolokia");
    enabled = properties.getProperty("enable", "true").equalsIgnoreCase("true");
  }

  public void start() {
    if (enabled) {
      startup = new Startup();
      Thread runner = new Thread(startup);
      runner.setDaemon(true);
      runner.start();
    }
  }

  public void stop(){
    if(startup != null){
      startup.stop();
    }
  }

  private class Startup implements Runnable {

    private JolokiaServer jolokiaServer;

    public void stop(){
      if (jolokiaServer != null) {
        try {
          jolokiaServer.stop();
        } catch (Exception e) {
          logger.log(LogMessages.JOLOKIA_SHUTDOWN_FAILURE, e);
        }
      }
    }

    public void run() {
      HashMap<String, String> map = new HashMap<>();
      for(Entry<Object, Object> entry: properties.entrySet()){
        map.put(entry.getKey().toString(), entry.getValue().toString());
      }
      JolokiaServerConfig config = new JolokiaServerConfig(map);
      try {
        jolokiaServer = new JolokiaServer(config, true);
        jolokiaServer.start();
      } catch (IOException e) {
        logger.log(LogMessages.JOLOKIA_STARTUP_FAILURE, e);
      }
    }
  }
}
