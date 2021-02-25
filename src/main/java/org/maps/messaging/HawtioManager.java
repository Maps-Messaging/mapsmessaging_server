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

package org.maps.messaging;

import static org.maps.logging.LogMessages.HAWTIO_INITIALISATION;
import static org.maps.logging.LogMessages.HAWTIO_NOT_CONFIGURED_TO_RUN;
import static org.maps.logging.LogMessages.HAWTIO_STARTUP;
import static org.maps.logging.LogMessages.HAWTIO_STARTUP_FAILURE;
import static org.maps.logging.LogMessages.HAWTIO_WAR_FILE_NOT_FOUND;

import java.io.File;
import java.lang.reflect.Method;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.configuration.ConfigurationManager;
import org.maps.utilities.configuration.ConfigurationProperties;

public class HawtioManager {

  private final Logger logger = LoggerFactory.getLogger(HawtioManager.class);

  private final ConfigurationProperties properties;
  private final String warFile;
  private final boolean enabled;


  public HawtioManager() {
    properties = ConfigurationManager.getInstance().getProperties("hawtio");
    System.setProperty("hawtio.authenticationEnabled", properties.getProperty("authenticationEnabled", "false"));
    String checkFile = properties.getProperty("warFileLocation", "");
    enabled = properties.getProperty("enable", "true").equalsIgnoreCase("true");
    if (enabled) {
      File winFile = new File(checkFile);
      if (winFile.exists()) {
        warFile = checkFile;
      } else {
        warFile = "";
      }
    } else {
      warFile = "";
    }
  }

  public void start() {
    if (enabled) {
      Thread runner = new Thread(new Startup());
      runner.setDaemon(true);
      runner.start();
    }
  }

  private class Startup implements Runnable {

    public void run() {
      if (properties.getProperty("enable").equalsIgnoreCase("true")) {
        logger.log(HAWTIO_STARTUP);
        if (warFile.length() > 0) {
          try {
            Class hawtioMain = Class.forName("io.hawt.embedded.Main");
            Object main = hawtioMain.getConstructor().newInstance();
            Method setWar = hawtioMain.getMethod("setWar", String.class);
            setWar.invoke(main, warFile);
            logger.log(HAWTIO_INITIALISATION, warFile);
            Method run = hawtioMain.getMethod("run");
            run.invoke(main);
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
