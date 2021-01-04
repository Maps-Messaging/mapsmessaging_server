package org.maps.utilities.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.ResourceList;

public class FilePropertyManager extends PropertyManager {

  private final Logger logger = LoggerFactory.getLogger(FilePropertyManager.class);

  public FilePropertyManager() {
    logger.log(LogMessages.PROPERTY_MANAGER_START);
  }

  protected void load(){
    try {
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*props"));
      for (String propertyName : knownProperties) {
        loadProperty(propertyName);
      }
    } catch (IOException e) {
      logger.log(LogMessages.PROPERTY_MANAGER_SCAN_FAILED, e);
    }
  }

  @Override
  protected void store(String name) {

  }

  @Override
  public void copy(PropertyManager propertyManager) {

  }

  private void loadProperty(String propertyName) {
    try {
      propertyName = propertyName.substring(propertyName.lastIndexOf(File.separatorChar) + 1);
      propertyName = propertyName.substring(0, propertyName.indexOf(".props"));
      ConfigurationProperties prop = loadFile(propertyName);
      properties.put(propertyName, scan(prop));
      logger.log(LogMessages.PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(LogMessages.PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private ConfigurationProperties loadFile(String propertyName) throws IOException {
    String propResourceName = "/" + propertyName;
    while (propResourceName.contains(".")) {
      propResourceName = propResourceName.replace('.', File.separatorChar);
    }
    propResourceName = propResourceName + ".props";
    InputStream is = getClass().getResourceAsStream(propResourceName);
    ConfigurationProperties response = new ConfigurationProperties();
    if (is != null) {
      response.load(is);
    } else {
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
    return response;
  }

  private @NotNull HashMap<Integer, ConfigurationProperties> scan(ConfigurationProperties props) {
    logger.log(LogMessages.PROPERTY_MANAGER_SCANNING, props.size());

    HashMap<Integer, ConfigurationProperties> list = new LinkedHashMap<>();

    HashMap<String, String> globals = scanForGlobalList(props);

    removeGlobalFromProperties(globals, props);

    //
    // Ok this property is a nested property and has multiple entries in it
    //
    if (props.isEmpty()) {
      list.put(0, new ConfigurationProperties(globals, null));
    } else {
      logger.log(LogMessages.PROPERTY_MANAGER_INDEX_DETECTED);
      LinkedHashMap<Integer, HashMap<String, String>> parsed = buildIndexedProperties(props);
      addGlobalKeys(parsed, globals, list);
    }
    return list;
  }

  private @NotNull LinkedHashMap<Integer, HashMap<String, String>> buildIndexedProperties(ConfigurationProperties props) {
    LinkedHashMap<Integer, HashMap<String, String>> parsed = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      String key = entry.getKey().toString();
      int index = key.lastIndexOf('_');
      if (index > 0) {
        String end = key.substring(index + 1).trim();
        int idx = Integer.parseInt(end);
        HashMap<String, String> tmpList = parsed.computeIfAbsent(idx, k -> new HashMap<>());
        String tmp = entry.getKey().toString();
        tmp = tmp.substring(0, index);
        tmpList.put(tmp, entry.getValue().toString());
      }
    }
    return parsed;
  }

  private void addGlobalKeys(
      LinkedHashMap<Integer, HashMap<String, String>> parsed,
      HashMap<String, String> globals,
      HashMap<Integer, ConfigurationProperties> list) {
    for (Map.Entry<Integer, HashMap<String, String>> entry : parsed.entrySet()) {
      HashMap<String, String> hashed = entry.getValue();
      list.put(entry.getKey(), new ConfigurationProperties(hashed, globals));
      logger.log(LogMessages.PROPERTY_MANAGER_COMPLETED_INDEX, hashed.size(), entry.getKey());
    }
  }

  private boolean isNumber(String key) {
    int index = key.lastIndexOf('_');
    if (index > 0) {
      String end = key.substring(index + 1).trim();
      try {
        Integer.parseInt(end);
      } catch (NumberFormatException e) {
        return false;
      }
    } else {
      return false;
    }
    return true;
  }

  //
// Create the global list of properties
//
  private @NotNull HashMap<String, String> scanForGlobalList(ConfigurationProperties props) {
    HashMap<String, String> globals = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      String key = entry.getKey().toString();
      if (!isNumber(key)) {
        globals.put(key, entry.getValue().toString());
      }
    }
    return globals;
  }

  //
// Remove the global from the list
//
  private void removeGlobalFromProperties(HashMap<String, String> globals, ConfigurationProperties props) {
    for (Map.Entry<String, String> global : globals.entrySet()) {
      props.remove(global.getKey());
    }
  }
}
