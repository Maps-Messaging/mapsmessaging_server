/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License.
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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.canbus.device.QueueFullPolicy;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;

public class CanbusConfig extends CanbusConfigDTO implements Config {

  public CanbusConfig(ConfigurationProperties config) {
    this.deviceName = config.getProperty("deviceName");

    if (config.containsKey("serial")) {
      this.serialConfig = new SerialConfig(config);
    } else {
      this.serialConfig = null;
    }

    this.queuedWritesEnabled = config.getBooleanProperty("queuedWritesEnabled", queuedWritesEnabled);
    this.queueDepth = config.getIntProperty("queueDepth", queueDepth);
    this.bitrateBitsPerSecond = config.getIntProperty("bitrateBitsPerSecond", bitrateBitsPerSecond);
    this.maxBusUsagePercent = config.getDoubleProperty("maxBusUsagePercent", maxBusUsagePercent);
    this.writeFailureBackoffMilliseconds = config.getLongProperty("writeFailureBackoffMilliseconds", writeFailureBackoffMilliseconds);
    this.queueFullPolicy = getQueueFullPolicy(config);


    setType("canbus");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof CanbusConfigDTO canbusConfig) {
      if (deviceName == null || !deviceName.equals(canbusConfig.getDeviceName())) {
        deviceName = canbusConfig.getDeviceName();
        hasChanged = true;
      }

      if (serialConfig == null && canbusConfig.getSerialConfig() != null) {
        serialConfig = canbusConfig.getSerialConfig();
        hasChanged = true;
      } else if (serialConfig != null && !serialConfig.equals(canbusConfig.getSerialConfig())) {
        serialConfig = canbusConfig.getSerialConfig();
        hasChanged = true;
      }

      if (queuedWritesEnabled != canbusConfig.isQueuedWritesEnabled()) {
        queuedWritesEnabled = canbusConfig.isQueuedWritesEnabled();
        hasChanged = true;
      }

      if (queueDepth != canbusConfig.getQueueDepth()) {
        queueDepth = canbusConfig.getQueueDepth();
        hasChanged = true;
      }

      if (bitrateBitsPerSecond != canbusConfig.getBitrateBitsPerSecond()) {
        bitrateBitsPerSecond = canbusConfig.getBitrateBitsPerSecond();
        hasChanged = true;
      }

      if (Double.compare(maxBusUsagePercent, canbusConfig.getMaxBusUsagePercent()) != 0) {
        maxBusUsagePercent = canbusConfig.getMaxBusUsagePercent();
        hasChanged = true;
      }

      if (queueFullPolicy != canbusConfig.getQueueFullPolicy()) {
        queueFullPolicy = canbusConfig.getQueueFullPolicy();
        hasChanged = true;
      }

      if (writeFailureBackoffMilliseconds != canbusConfig.getWriteFailureBackoffMilliseconds()) {
        writeFailureBackoffMilliseconds = canbusConfig.getWriteFailureBackoffMilliseconds();
        hasChanged = true;
      }
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();

    config.put("deviceName", deviceName);

    if (serialConfig instanceof Config serialConfiguration) {
      config.put("serial", serialConfiguration.toConfigurationProperties());
    }

    config.put("queuedWritesEnabled", queuedWritesEnabled);
    config.put("queueDepth", queueDepth);
    config.put("bitrateBitsPerSecond", bitrateBitsPerSecond);
    config.put("maxBusUsagePercent", maxBusUsagePercent);
    config.put("queueFullPolicy", queueFullPolicy.name());
    config.put("writeFailureBackoffMilliseconds", writeFailureBackoffMilliseconds);

    return config;
  }

  private QueueFullPolicy getQueueFullPolicy(ConfigurationProperties config) {
    String queueFullPolicyName = config.getProperty("queueFullPolicy");

    if (queueFullPolicyName == null || queueFullPolicyName.isBlank()) {
      return queueFullPolicy;
    }

    return QueueFullPolicy.valueOf(queueFullPolicyName.trim().toUpperCase());
  }
}