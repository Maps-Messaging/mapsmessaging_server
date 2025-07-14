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

package io.mapsmessaging;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import static io.mapsmessaging.logging.ServerLogMessages.MAP_ENV_DATA_RESOLVED;
import static io.mapsmessaging.logging.ServerLogMessages.MAP_ENV_HOME_RESOLVED;

public final class MapsEnvironment {

  private static final String ENV_MAPS_HOME = "MAPS_HOME";
  private static final String ENV_MAPS_DATA = "MAPS_DATA";

  private static final Logger logger = LoggerFactory.getLogger(MapsEnvironment.class);

  public static String getMapsHome() {
    String home = System.getProperty(ENV_MAPS_HOME, System.getenv(ENV_MAPS_HOME));
    if (home != null && !home.isBlank()) {
      logger.log(MAP_ENV_HOME_RESOLVED, home, "system/env");
      return home;
    }

    try {
      String path = new File(MapsEnvironment.class.getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .toURI())
          .getParentFile()
          .getAbsolutePath();
      logger.log(MAP_ENV_HOME_RESOLVED, path, "codeSource");
      return path;
    } catch (Exception e) {
      String fallback = Paths.get(".").toAbsolutePath().normalize().toString();
      logger.log(MAP_ENV_HOME_RESOLVED, fallback, "fallback");
      return fallback;
    }
  }

  public static String getMapsData() {
    String data = System.getProperty(ENV_MAPS_DATA, System.getenv(ENV_MAPS_DATA));
    if (data != null && !data.isBlank()) {
      if(data.toLowerCase().contains("${programdata}")){
        // Let's do the lookup / replace
        String programData = System.getenv("ProgramData");
        data = data.replaceAll("(?i)\\$\\{programdata}", Matcher.quoteReplacement(programData));
      }
      logger.log(MAP_ENV_DATA_RESOLVED, data, "system/env");
      return data;
    }

    String os = System.getProperty("os.name").toLowerCase();
    String resolved;

    if (os.contains("win")) {
      String programData = System.getenv("ProgramData");
      resolved = (programData != null)
          ? programData + File.separator + "MapsMessaging"
          : getMapsHome() + File.separator + "data";
    } else if (os.contains("mac")) {
      resolved = System.getProperty("user.home") + "/Library/Application Support/MapsMessaging";
    } else {
      resolved = "/opt/maps_data";
    }

    logger.log(MAP_ENV_DATA_RESOLVED, resolved, os);
    return resolved;
  }

  private MapsEnvironment() {
  }
}
