/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Destination Configuration")
public class DestinationConfig {


  private static final String OPTIONAL_PATH = "{folder}";

  private boolean sync;
  private boolean debug;
  private boolean remap;
  private int itemCount;
  private int expiredEventPoll;
  private int autoPauseTimeout;
  private long maxPartitionSize;
  private String trailingPath;
  private String name;
  private String directory;
  private String namespace;
  private String type;
  private FormatConfig format;
  private CacheConfig cache;
  private ArchiveConfig archive;
  private String namespaceMapping;

  public DestinationConfig(ConfigurationProperties properties) {
    debug = properties.getBooleanProperty("debug", false);
    name = properties.getProperty("name", "");
    directory = properties.getProperty("directory", "");
    namespace = properties.getProperty("namespace", "");
    type = properties.getProperty("type", "");
    sync = properties.getProperty("sync", "disable").equalsIgnoreCase("enable");
    itemCount = properties.getIntProperty("itemCount", 100);
    maxPartitionSize = properties.getLongProperty("maxPartitionSize", 4096L);
    expiredEventPoll = properties.getIntProperty("expiredEventPoll", 20);
    autoPauseTimeout = properties.getIntProperty("autoPauseTimeout", 300);

    if (properties.containsKey("format")) {
      format = new FormatConfig((ConfigurationProperties) properties.get("format"));
    }
    if (properties.containsKey("cache")) {
      cache = new CacheConfig((ConfigurationProperties) properties.get("cache"));
    }
    if (properties.containsKey("archive")) {
      archive = new ArchiveConfig((ConfigurationProperties) properties.get("archive"));
    }
    String propertyNamespace = namespace;
    remap = (propertyNamespace.endsWith(OPTIONAL_PATH) && directory.contains(OPTIONAL_PATH));
    if (remap) {
      namespaceMapping = propertyNamespace.substring(0, propertyNamespace.indexOf(OPTIONAL_PATH));
    } else {
      namespaceMapping = propertyNamespace;
    }
  }


  public String getTrailingPath() {
    if (remap) {
      return directory.substring(directory.indexOf(OPTIONAL_PATH) + OPTIONAL_PATH.length());
    }
    return "";
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", name);
    properties.put("debug", debug);
    properties.put("directory", directory);
    properties.put("namespace", namespace);
    properties.put("type", type);
    properties.put("sync", sync?"enabled":"disable");
    properties.put("itemCount", itemCount);
    properties.put("maxPartitionSize", maxPartitionSize);
    properties.put("expiredEventPoll", expiredEventPoll);
    properties.put("autoPauseTimeout", autoPauseTimeout);
    if (format != null) {
      properties.put("format", format.toConfigurationProperties());
    }
    if (cache != null) {
      properties.put("cache", cache.toConfigurationProperties());
    }
    if (archive != null) {
      properties.put("archive", archive.toConfigurationProperties());
    }
    return properties;
  }
}

