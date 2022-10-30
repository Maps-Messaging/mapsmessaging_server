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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.yaml.snakeyaml.Yaml;

public abstract class YamlPropertyManager extends PropertyManager {

  private static final String GLOBAL = "global";

  protected void parseAndLoadYaml(String propertyName, String yamlString) throws IOException {
    Yaml yaml = new Yaml();
    JsonParser parser = new YamlParser(yaml.load(yamlString));
    Map<String, Object> response = parser.parse();
    Object topLevel = response.get(propertyName);
    if (topLevel instanceof Map) {
      Map<String, Object> root = (Map<String, Object>) topLevel;
      root.put("loaded", System.currentTimeMillis());
    }
    ConfigurationProperties configurationProperties = new ConfigurationProperties();
    for (Entry<String, Object> item : response.entrySet()) {
      Map<String, Object> entry = (Map<String, Object>) item.getValue();
      if (entry.get("global") != null) {
        Map<String, Object> global = (Map<String, Object>) entry.remove("global");
        configurationProperties.setGlobal(new ConfigurationProperties(global));
      }
      configurationProperties.putAll(entry);
    }
    configurationProperties.setSource(yamlString);
    properties.put(propertyName, configurationProperties);
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
