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

  /**
   * Resolves the MAPS_DATA directory location for storing server data.
   * <p>
   * Resolution logic follows this order:
   * <ol>
   *   <li>If the system property or environment variable {@code maps_data} is set, use it.
   *       <ul>
   *         <li>If the value contains {@code ${ProgramData}} (case-insensitive), it will be replaced with the actual
   *         system environment variable {@code ProgramData} (Windows only).</li>
   *       </ul>
   *   </li>
   *   <li>Otherwise, check if the server is running from an "installed" directory:
   *       <ul>
   *         <li>Linux: paths under {@code /opt/}, {@code /usr/}</li>
   *         <li>macOS: paths under {@code /Applications/}</li>
   *         <li>Windows: paths containing {@code Program Files} or {@code ProgramData}</li>
   *       </ul>
   *   </li>
   *   <li>If running from an installed location, use OS-specific defaults:
   *       <ul>
   *         <li>Windows: {@code %ProgramData%\MapsMessaging}</li>
   *         <li>macOS: {@code ~/Library/Application Support/MapsMessaging}</li>
   *         <li>Linux: {@code /opt/maps_data}</li>
   *       </ul>
   *   </li>
   *   <li>If not installed (e.g. running from a dev tree, ZIP, or tar.gz), fallback to:
   *       <ul>
   *         <li>{@code $MAPS_HOME/data}</li>
   *       </ul>
   *   </li>
   * </ol>
   * This approach ensures write-access for dev/test environments while using platform-standard locations in production installs.
   *
   * @return Resolved absolute path to the MAPS_DATA directory
   */
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
    String mapsHome = getMapsHome();
    boolean installed = isInstalledPath(mapsHome);

    if (!installed) {
      resolved = mapsHome + File.separator + "data";
      logger.log(MAP_ENV_DATA_RESOLVED, resolved, "non-installed");
      return resolved;
    }

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

  private static boolean isInstalledPath(String home) {
    if (home == null) return false;
    String path = home.toLowerCase();
    return path.startsWith("/opt/")
        || path.startsWith("/usr/")
        || path.startsWith("/applications/")
        || path.contains("program files")
        || path.contains("programdata");
  }

  private MapsEnvironment() {
  }
}
