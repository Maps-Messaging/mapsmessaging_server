package io.mapsmessaging;

import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.SystemProperties;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Getter
public class EnvironmentConfig {
  private final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

  private final String homeDirectory;
  private final String dataDirectory;
  private final File homePath;
  private final File dataPath;

  public EnvironmentConfig() throws IOException {
    homeDirectory = loadAndCreatePath("MAPS_HOME", ".", false);
    dataDirectory = loadAndCreatePath("MAPS_DATA", homeDirectory + "data", true);
    homePath = new File(homeDirectory);
    dataPath = new File(dataDirectory);
    logger.log(ServerLogMessages.MESSAGE_DAEMON_HOME_DIRECTORY, homeDirectory);
  }

  private String loadAndCreatePath(String name, String defaultPath, boolean create) throws IOException {
    String directoryPath = SystemProperties.getInstance().getProperty(name, defaultPath);
    File testPath = new File(directoryPath);
    if (!testPath.exists()) {
      if (create) {
        Files.createDirectories(testPath.toPath());

      } else {
        logger.log(ServerLogMessages.MESSAGE_DAEMON_NO_HOME_DIRECTORY, directoryPath);
        directoryPath = defaultPath;
      }
    }
    if (!directoryPath.endsWith(File.separator)) {
      directoryPath = defaultPath + File.separator;
    }
    return directoryPath;
  }
}
