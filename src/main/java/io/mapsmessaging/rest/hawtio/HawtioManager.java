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

import io.hawt.embedded.Main;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.configuration.EnvironmentConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.io.File;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class HawtioManager implements Agent {

  private final Logger logger = LoggerFactory.getLogger(HawtioManager.class);

  private final ConfigurationProperties properties;
  private final String warFile;
  private final boolean enabled;


  public HawtioManager() {
    properties = ConfigurationManager.getInstance().getProperties("hawtio");
    System.setProperty("hawtio.authenticationEnabled", "" + properties.getBooleanProperty("authenticationEnabled", false));

    String checkFile = properties.getProperty("warFileLocation", "");
    enabled = properties.getBooleanProperty("enable", true) && isJolokiaEnabled();

    checkFile = EnvironmentConfig.getInstance().translatePath(checkFile);
    if (enabled) {
      File winFile = new File(checkFile);
      if (winFile.exists()) {
        warFile = checkFile;
      } else {
        warFile = scanForWarFileInLib();
      }
    } else {
      warFile = "";
    }
  }

  private String scanForWarFileInLib() {
    String libString = EnvironmentConfig.getInstance().getPathLocations().get("MAPS_HOME") + "/lib";
    libString = libString.replace("//", "/");
    File file = new File(libString);
    if (file.isDirectory()) {
      String[] libraries = file.list();
      if (libraries != null) {
        for (String library : libraries) {
          String test = library.toLowerCase().trim();
          if (test.contains("hawtio-default-") && test.endsWith(".war")) {
            return file.getAbsolutePath() + File.separator + library;
          }
        }
      }
    }
    return "";
  }

  private boolean isJolokiaEnabled() {
    ConfigurationProperties jolokia = ConfigurationManager.getInstance().getProperties("jolokia");
    return jolokia.getBooleanProperty("enable", true);
  }

  @Override
  public String getName() {
    return "Hawtio Manager";
  }

  @Override
  public String getDescription() {
    return "Hawtio Web management";
  }

  public void start() {
    if (enabled) {
      Thread runner = new Thread(new Startup());
      runner.setDaemon(true);
      runner.start();
    }
  }

  @Override
  public void stop() {
    // nothing to stop
  }


  private class Startup implements Runnable {

    private void register() {
      if (properties.getBooleanProperty("discoverable", false)) {
        String service = "_http._tcp.local";
        try {
          MessageDaemon.getInstance().getDiscoveryManager().register(properties.getProperty("hostname", "0.0.0.0"), service, "hawtio", 8080, "/hawtio/");
        } catch (Exception e) {
          logger.log(ServerLogMessages.HAWTIO_REGISTRATION_FAILED, e);
        }
      }
    }

    public void run() {
      if (properties.getBooleanProperty("enable", false)) {
        logger.log(HAWTIO_STARTUP);
        ConfigurationProperties config = (ConfigurationProperties) properties.get("config");
        if (!warFile.isEmpty()) {
          try {
            for (Map.Entry<String, Object> entry : config.entrySet()) {
              System.setProperty(entry.getKey(), entry.getValue().toString());
            }
            int port = config.getIntProperty("hawtio.port", 8181);
            Main main = new Main();
            main.setPort(port);
            main.setWar(warFile);
            main.run();
            register();
          } catch (Exception e) {
            logger.log(HAWTIO_STARTUP_FAILURE, e);
          }
        } else {
          logger.log(HAWTIO_WAR_FILE_NOT_FOUND, warFile);
        }
      } else {
        logger.log(HAWTIO_NOT_CONFIGURED_TO_RUN);
      }
    }
  }
}
