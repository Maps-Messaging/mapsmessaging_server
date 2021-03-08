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

package org.maps.utilities.configuration;

import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.ResourceList;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class YamlPropertyManager extends PropertyManager {
  private static final String GLOBAL = "global";

  private final Logger logger = LoggerFactory.getLogger(YamlPropertyManager.class);

  @Override
  protected void load(){
    try {
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*yaml"));
      for (String propertyName : knownProperties) {
        loadProperty(propertyName);
      }
    } catch (IOException e) {
      logger.log(LogMessages.PROPERTY_MANAGER_SCAN_FAILED, e);
    }
  }

  private void loadProperty(String propertyName) {
    try {
      propertyName = propertyName.substring(propertyName.lastIndexOf(File.separatorChar) + 1);
      propertyName = propertyName.substring(0, propertyName.indexOf(".yaml"));
      Map<String, Object> map = loadFile(propertyName);
      Object objRoot = map.get(propertyName);
      if(objRoot instanceof ConfigurationProperties) {
        ConfigurationProperties root = (ConfigurationProperties)objRoot;
        Object global = root.get(GLOBAL);
        Object data = root.get("data");
        if (data != null && global instanceof ConfigurationProperties) {
          if (data instanceof List) {
            for (ConfigurationProperties properties : (List<ConfigurationProperties>) data) {
              properties.setGlobal((ConfigurationProperties) global);
            }
          } else if (data instanceof ConfigurationProperties) {
            ((ConfigurationProperties) data).setGlobal((ConfigurationProperties) global);
          }
          root.remove(GLOBAL);
        }
      }

      properties.putAll(map);
      logger.log(LogMessages.PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(LogMessages.PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private  Map<String, Object> loadFile(String propertyName) throws IOException {
    String propResourceName = "/" + propertyName;
    while (propResourceName.contains(".")) {
      propResourceName = propResourceName.replace('.', File.separatorChar);
    }
    propResourceName = propResourceName + ".yaml";
    InputStream is = getClass().getResourceAsStream(propResourceName);
    Map<String, Object> response;
    if (is != null) {
      Yaml yaml = new Yaml();
      JSONParser parser = new YamlParser(yaml.load(is));
      is.close();
      response = parser.parse();
      Object topLevel = response.get(propertyName);
      if(topLevel instanceof Map){
        Map<String, Object> root = (Map<String, Object>) topLevel;
        root.put("JSON", parser.getJson());
        root.put("loaded", System.currentTimeMillis());
      }
    } else {
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
    return response;
  }

  @Override
  protected void store(String name) throws IOException {
    HashMap<String, Object> data = new LinkedHashMap<>(properties);
    if(properties.getGlobal() != null) {
      data.put(GLOBAL, new LinkedHashMap<>(properties.getGlobal()));
    }
    try(PrintWriter writer = new PrintWriter(name)) {
      Yaml yaml = new Yaml();
      yaml.dump(data, writer);
    }
  }

  @Override
  public void copy(PropertyManager propertyManager) {
    HashMap<String, Object> data = new LinkedHashMap<>(propertyManager.properties);
    properties.clear();
    properties.putAll(data);
    properties.setGlobal(properties.getGlobal());
  }
}
