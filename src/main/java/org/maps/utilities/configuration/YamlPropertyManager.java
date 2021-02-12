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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.ResourceList;
import org.yaml.snakeyaml.Yaml;

public class YamlPropertyManager extends PropertyManager {
  private final Logger logger = LoggerFactory.getLogger(YamlPropertyManager.class);

  public YamlPropertyManager(){ }

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
        Object global = root.get("global");
        Object data = root.get("data");
        if (data != null && global instanceof ConfigurationProperties) {
          if (data instanceof List) {
            for (ConfigurationProperties properties : (List<ConfigurationProperties>) data) {
              properties.setGlobal((ConfigurationProperties) global);
            }
          } else if (data instanceof ConfigurationProperties) {
            ((ConfigurationProperties) data).setGlobal((ConfigurationProperties) global);
          }
          root.remove("global");
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
  protected void store(String name) {

  }

  @Override
  public void copy(PropertyManager propertyManager) {

  }
}
