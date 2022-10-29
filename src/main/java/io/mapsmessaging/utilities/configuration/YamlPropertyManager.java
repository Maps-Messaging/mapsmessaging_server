/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

public class YamlPropertyManager extends PropertyManager {

  private static final String GLOBAL = "global";

  private final Logger logger = LoggerFactory.getLogger(YamlPropertyManager.class);

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
      Map<String, Object> map = loadFile(propertyName);
      String source = (String) map.remove("yaml");
      ConfigurationProperties configurationProperties = new ConfigurationProperties();
      for (Entry<String, Object> item : map.entrySet()) {
        Map<String, Object> entry = (Map<String, Object>) item.getValue();
        if (entry.get("global") != null) {
          Map<String, Object> global = (Map<String, Object>) entry.remove("global");
          configurationProperties.setGlobal(new ConfigurationProperties(global));
        }
        configurationProperties.putAll(entry);
      }
      configurationProperties.setSource(source);
      properties.put(propertyName, configurationProperties);
      logger.log(ServerLogMessages.PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(ServerLogMessages.PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private Map<String, Object> loadFile(String propertyName) throws IOException {
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
      String tmp = byteArrayOutputStream.toString();
      Yaml yaml = new Yaml();
      JsonParser parser = new YamlParser(yaml.load(tmp));
      is.close();
      response = parser.parse();
      Object topLevel = response.get(propertyName);
      if (topLevel instanceof Map) {
        Map<String, Object> root = (Map<String, Object>) topLevel;
        root.put("loaded", System.currentTimeMillis());
      }
      response.put("yaml", tmp);
    } else {
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
    return response;
  }

  @Override
  protected void store(String name) throws IOException {
    HashMap<String, Object> data = new LinkedHashMap<>(properties.getMap());
    if (properties.getGlobal() != null) {
      data.put(GLOBAL, new LinkedHashMap<>(properties.getGlobal().getMap()));
    }
    try (PrintWriter writer = new PrintWriter(name)) {
      Yaml yaml = new Yaml();
      yaml.dump(data, writer);
    }
  }

  @Override
  public void copy(PropertyManager propertyManager) {
    HashMap<String, Object> data = new LinkedHashMap<>(propertyManager.properties.getMap());
    properties.clear();
    properties.putAll(data);
    properties.setGlobal(properties.getGlobal());
  }
}
