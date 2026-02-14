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

import io.mapsmessaging.config.aggregator.AggregatorConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.AggregatorManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class AggregatorManagerConfig extends AggregatorManagerConfigDTO implements Config, ConfigManager {

  private AggregatorManagerConfig(ConfigurationProperties config) {
    stripeCount = readInt(config, "stripeCount", stripeCount);
    maxBatchPerAggregator = readInt(config, "maxBatchPerAggregator", maxBatchPerAggregator);
    idleSleepMs = readInt(config, "idleSleepMs", idleSleepMs);
    mailboxCapacity = readInt(config, "mailboxCapacity", mailboxCapacity);
    maxAggregators = readInt(config, "maxAggregators", maxAggregators);

    aggregatorConfigList = new ArrayList<>();
    Object dataObject = config.get("data");
    if (dataObject instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) dataObject) {
        aggregatorConfigList.add(new AggregatorConfig(entry));
      }
    } else if (dataObject instanceof ConfigurationProperties configurationProperties) {
      aggregatorConfigList.add(new AggregatorConfig(configurationProperties));
    }
  }

  public static AggregatorManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(AggregatorManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof AggregatorManagerConfigDTO newConfig) {

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

    for (AggregatorConfigDTO aggregatorConfigDTO : aggregatorConfigList) {
      dataList.add(((AggregatorConfig) aggregatorConfigDTO).toConfigurationProperties());
    }

    configurationProperties.put("data", dataList);
    return configurationProperties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new AggregatorManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }

  @Override
  public String getName() {
    return "AggregatorManager";
  }

  private int readInt(ConfigurationProperties config, String key, int defaultValue) {
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
