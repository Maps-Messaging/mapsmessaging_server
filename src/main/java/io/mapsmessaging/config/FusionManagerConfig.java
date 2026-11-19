/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.config;

import io.mapsmessaging.config.fusion.FusionConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.FusionManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class FusionManagerConfig extends FusionManagerConfigDTO implements Config, ConfigManager {

  private FusionManagerConfig(ConfigurationProperties fullConfig) {
    ConfigurationProperties config = (ConfigurationProperties) fullConfig.get("data");
    stripeCount = readInt(config, "stripeCount", stripeCount);
    maxBatchPerAggregator = readInt(config, "maxBatchPerAggregator", maxBatchPerAggregator);
    idleSleepMs = readInt(config, "idleSleepMs", idleSleepMs);
    mailboxCapacity = readInt(config, "mailboxCapacity", mailboxCapacity);
    maxAggregators = readInt(config, "maxAggregators", maxAggregators);

    aggregatorConfigList = new ArrayList<>();
    if(config != null) {
      Object dataObject = config.get("aggregatorConfigList");
      if (dataObject instanceof List) {
        for (ConfigurationProperties entry : (List<ConfigurationProperties>) dataObject) {
          aggregatorConfigList.add(new FusionConfig(entry));
        }
      } else if (dataObject instanceof ConfigurationProperties configurationProperties) {
        aggregatorConfigList.add(new FusionConfig(configurationProperties));
      }
    }
  }

  public static FusionManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(FusionManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof FusionManagerConfigDTO newConfig) {

      if (this.stripeCount != newConfig.getStripeCount()) {
        this.stripeCount = newConfig.getStripeCount();
        hasChanged = true;
      }

      if (this.maxBatchPerAggregator != newConfig.getMaxBatchPerAggregator()) {
        this.maxBatchPerAggregator = newConfig.getMaxBatchPerAggregator();
        hasChanged = true;
      }

      if (this.idleSleepMs != newConfig.getIdleSleepMs()) {
        this.idleSleepMs = newConfig.getIdleSleepMs();
        hasChanged = true;
      }

      if (this.mailboxCapacity != newConfig.getMailboxCapacity()) {
        this.mailboxCapacity = newConfig.getMailboxCapacity();
        hasChanged = true;
      }

      if (this.maxAggregators != newConfig.getMaxAggregators()) {
        this.maxAggregators = newConfig.getMaxAggregators();
        hasChanged = true;
      }

      if (this.aggregatorConfigList.size() != newConfig.getAggregatorConfigList().size()) {
        this.aggregatorConfigList = newConfig.getAggregatorConfigList();
        hasChanged = true;
      } else {
        for (int index = 0; index < this.aggregatorConfigList.size(); index++) {
          if (!this.aggregatorConfigList.get(index).equals(newConfig.getAggregatorConfigList().get(index))) {
            this.aggregatorConfigList.set(index, newConfig.getAggregatorConfigList().get(index));
            hasChanged = true;
          }
        }
      }
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties configurationProperties = new ConfigurationProperties();
    List<ConfigurationProperties> dataList = new ArrayList<>();

    configurationProperties.put("stripeCount", stripeCount);
    configurationProperties.put("maxBatchPerAggregator", maxBatchPerAggregator);
    configurationProperties.put("idleSleepMs", idleSleepMs);
    configurationProperties.put("mailboxCapacity", mailboxCapacity);
    configurationProperties.put("maxAggregators", maxAggregators);

    aggregatorConfigList.stream()
        .map(aggregatorConfigDTO -> ((FusionConfig) aggregatorConfigDTO).toConfigurationProperties())
        .forEachOrdered(dataList::add);

    configurationProperties.put("data", dataList);
    return configurationProperties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    ConfigurationManager configurationManager = ConfigurationManager.getInstance();
    ConfigurationProperties properties = configurationManager.getProperties(getName());
    // A renamed legacy YAML file can still contain its original top-level key.
    java.nio.file.Path configDirectory = java.nio.file.Path.of("conf");
    boolean bothPresent = java.nio.file.Files.exists(configDirectory.resolve("AggregatorManager.yaml"))
        && java.nio.file.Files.exists(configDirectory.resolve("FusionManager.yaml"));
    if (!bothPresent && !(properties.get("data") instanceof ConfigurationProperties)) {
      ConfigurationProperties legacy = configurationManager.getProperties("AggregatorManager");
      if (legacy.get("data") instanceof ConfigurationProperties) {
        properties = legacy;
      }
    }
    return new FusionManagerConfig(properties);
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "FusionManager";
  }

  private int readInt(ConfigurationProperties config, String key, int defaultValue) {
    if (config == null) {
      return defaultValue;
    }
    Object value = config.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String str) {
      try {
        return Integer.parseInt(str.trim());
      } catch (NumberFormatException ignored) {
        return defaultValue;
      }
    }
    return defaultValue;
  }
}
