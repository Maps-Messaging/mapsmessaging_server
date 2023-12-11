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

package io.mapsmessaging;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.SystemProperties;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Getter
public class EnvironmentConfig {
  private static final String MAPS_HOME = "MAPS_HOME";
  private static final String MAPS_DATA = "MAPS_DATA";

  private final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

  private final String homeDirectory;
  private final String dataDirectory;
  private final File homePath;
  private final File dataPath;

  public EnvironmentConfig() throws IOException {
    homeDirectory = loadAndCreatePath(MAPS_HOME, ".", false);
    dataDirectory = loadAndCreatePath(MAPS_DATA, homeDirectory + "data", true);
    homePath = new File(homeDirectory);
    dataPath = new File(dataDirectory);
    logger.log(ServerLogMessages.MESSAGE_DAEMON_HOME_DIRECTORY, homeDirectory);
  }

  private String loadAndCreatePath(String name, String defaultPath, boolean create) throws IOException {
    String directoryPath = SystemProperties.getInstance().locateProperty(name, defaultPath);
    File testPath = new File(directoryPath);
    if (!testPath.exists()) {
      if (create) {
        Files.createDirectories(testPath.toPath());

      } else {
        logger.log(ServerLogMessages.MESSAGE_DAEMON_NO_HOME_DIRECTORY, directoryPath);
      }
    }
    if (!directoryPath.endsWith(File.separator)) {
      directoryPath = directoryPath + File.separator;
    }
    return directoryPath;
  }


  public String translatePath(String path) {
    String updated = path;
    updated = updated.replace("{{" + MAPS_DATA + "}}", dataDirectory);
    updated = updated.replace("{{" + MAPS_HOME + "}}", homeDirectory);

    while (updated.contains("{{") && updated.contains("}}")) {
      updated = scanForNonStandardSub(updated);
    }
    while (updated.contains("//")) {
      updated = updated.replace("//", File.separator);
    }
    while (updated.contains("\\\\")) {
      updated = updated.replace("\\\\", File.separator);
    }
    while (updated.contains("/\\")) {
      updated = updated.replace("/\\", File.separator);
    }
    while (updated.contains("\\/")) {
      updated = updated.replace("\\/", File.separator);
    }
    return updated;
  }

  private String scanForNonStandardSub(String path) {
    int start = path.indexOf("{{");
    int end = path.indexOf("}}");
    String env = path.substring(start + 2, end);
    String located = SystemProperties.getInstance().locateProperty(env, "");
    path = path.replace("{{" + env + "}}", located);
    while (path.contains("\"")) {
      path = path.replace("\"", "");
    }
    return path;
  }

}
