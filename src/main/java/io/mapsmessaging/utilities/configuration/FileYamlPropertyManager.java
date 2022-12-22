package io.mapsmessaging.utilities.configuration;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.ResourceList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public class FileYamlPropertyManager extends YamlPropertyManager {

  private final Logger logger = LoggerFactory.getLogger(FileYamlPropertyManager.class);

  @Override
  protected void load() {
    try {
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*yaml"));
      for (String propertyName : knownProperties) {
        loadProperty(propertyName);
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.PROPERTY_MANAGER_SCAN_FAILED, e);
    }
  }

  private void loadProperty(String propertyName) {
    try {
      propertyName = propertyName.substring(propertyName.lastIndexOf(File.separatorChar) + 1);
      propertyName = propertyName.substring(0, propertyName.indexOf(".yaml"));
      loadFile(propertyName);
      logger.log(ServerLogMessages.PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(ServerLogMessages.PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private void loadFile(String propertyName) throws IOException {
    String propResourceName = "/" + propertyName;
    while (propResourceName.contains(".")) {
      propResourceName = propResourceName.replace('.', File.separatorChar);
    }
    propResourceName = propResourceName + ".yaml";
    InputStream is = getClass().getResourceAsStream(propResourceName);
    Map<String, Object> response;
    if (is != null) {
      int read = 1;
      byte[] buffer = new byte[1024];
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      while (read > 0) {
        read = is.read(buffer);
        if (read > 0) {
          byteArrayOutputStream.write(buffer, 0, read);
        }
      }
      is.close();
      parseAndLoadYaml(propertyName, byteArrayOutputStream.toString());
    } else {
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
  }

}