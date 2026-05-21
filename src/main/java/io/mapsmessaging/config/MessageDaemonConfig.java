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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor
public class MessageDaemonConfig extends MessageDaemonConfigDTO implements Config, ConfigManager {

  private MessageDaemonConfig(ConfigurationProperties config) {
    this.delayedPublishInterval = config.getIntProperty("DelayedPublishInterval", delayedPublishInterval);
    this.sessionPipeLines = config.getIntProperty("SessionPipeLines", sessionPipeLines);
    this.transactionExpiry = config.getLongProperty("TransactionExpiry", transactionExpiry);
    this.transactionScan = config.getLongProperty("TransactionScan", transactionScan);
    this.compressionName = config.getProperty("CompressionName", compressionName);
    this.compressMessageMinSize = config.getIntProperty("CompressMessageMinSize", compressMessageMinSize);
    this.incrementPriorityMethod = config.getProperty("IncrementPriorityMethod", incrementPriorityMethod);
    this.enableResourceStatistics = config.getBooleanProperty("EnableResourceStatistics", enableResourceStatistics);
    this.enableSystemTopics = config.getBooleanProperty("EnableSystemTopics", enableSystemTopics);
    this.enableSystemStatusTopics = config.getBooleanProperty("EnableSystemStatusTopics", enableSystemStatusTopics);
    this.enableSystemTopicAverages = config.getBooleanProperty("EnableSystemTopicAverages", enableSystemTopicAverages);
    this.enableJMX = config.getBooleanProperty("EnableJMX", enableJMX);
    this.enableJMXStatistics = config.getBooleanProperty("EnableJMXStatistics", enableJMXStatistics);
    this.tagMetaData = config.getBooleanProperty("tagMetaData", tagMetaData);
    this.latitude = config.getDoubleProperty("latitude", latitude);
    this.longitude = config.getDoubleProperty("longitude", longitude);
    this.sendAnonymousStatusUpdates = config.getBooleanProperty("SendAnonymousStatusUpdates", sendAnonymousStatusUpdates);
    this.exitOnConfigError = config.getBooleanProperty("exitOnConfigError", exitOnConfigError);
  }

  public static MessageDaemonConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(MessageDaemonConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof MessageDaemonConfigDTO)) {
      return false;
    }

    MessageDaemonConfigDTO newConfig = (MessageDaemonConfigDTO) config;
    boolean hasChanged = false;

    if (this.delayedPublishInterval != newConfig.getDelayedPublishInterval()) {
      this.delayedPublishInterval = newConfig.getDelayedPublishInterval();
      hasChanged = true;
    }
    if (this.sessionPipeLines != newConfig.getSessionPipeLines()) {
      this.sessionPipeLines = newConfig.getSessionPipeLines();
      hasChanged = true;
    }
    if (this.transactionExpiry != newConfig.getTransactionExpiry()) {
      this.transactionExpiry = newConfig.getTransactionExpiry();
      hasChanged = true;
    }
    if (this.transactionScan != newConfig.getTransactionScan()) {
      this.transactionScan = newConfig.getTransactionScan();
      hasChanged = true;
    }
    if (!this.compressionName.equals(newConfig.getCompressionName())) {
      this.compressionName = newConfig.getCompressionName();
      hasChanged = true;
    }
    if (this.compressMessageMinSize != newConfig.getCompressMessageMinSize()) {
      this.compressMessageMinSize = newConfig.getCompressMessageMinSize();
      hasChanged = true;
    }
    if (!this.incrementPriorityMethod.equals(newConfig.getIncrementPriorityMethod())) {
      this.incrementPriorityMethod = newConfig.getIncrementPriorityMethod();
      hasChanged = true;
    }
    if (this.sendAnonymousStatusUpdates != newConfig.isSendAnonymousStatusUpdates()) {
      this.sendAnonymousStatusUpdates = newConfig.isSendAnonymousStatusUpdates();
      hasChanged = true;
    }
    if (this.enableResourceStatistics != newConfig.isEnableResourceStatistics()) {
      this.enableResourceStatistics = newConfig.isEnableResourceStatistics();
      hasChanged = true;
    }
    if (this.enableSystemTopics != newConfig.isEnableSystemTopics()) {
      this.enableSystemTopics = newConfig.isEnableSystemTopics();
      hasChanged = true;
    }
    if (this.enableSystemStatusTopics != newConfig.isEnableSystemStatusTopics()) {
      this.enableSystemStatusTopics = newConfig.isEnableSystemStatusTopics();
      hasChanged = true;
    }
    if (this.enableSystemTopicAverages != newConfig.isEnableSystemTopicAverages()) {
      this.enableSystemTopicAverages = newConfig.isEnableSystemTopicAverages();
      hasChanged = true;
    }
    if (this.enableJMX != newConfig.isEnableJMX()) {
      this.enableJMX = newConfig.isEnableJMX();
      hasChanged = true;
    }
    if (this.enableJMXStatistics != newConfig.isEnableJMXStatistics()) {
      this.enableJMXStatistics = newConfig.isEnableJMXStatistics();
      hasChanged = true;
    }
    if (this.tagMetaData != newConfig.isTagMetaData()) {
      this.tagMetaData = newConfig.isTagMetaData();
      hasChanged = true;
    }
    if (this.latitude != newConfig.getLatitude()) {
      this.latitude = newConfig.getLatitude();
      hasChanged = true;
    }
    if (this.longitude != newConfig.getLongitude()) {
      this.longitude = newConfig.getLongitude();
      hasChanged = true;
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("DelayedPublishInterval", this.delayedPublishInterval);
    config.put("SessionPipeLines", this.sessionPipeLines);
    config.put("TransactionExpiry", this.transactionExpiry);
    config.put("TransactionScan", this.transactionScan);
    config.put("CompressionName", this.compressionName);
    config.put("CompressMessageMinSize", this.compressMessageMinSize);
    config.put("EnableResourceStatistics", this.enableResourceStatistics);
    config.put("EnableSystemTopics", this.enableSystemTopics);
    config.put("EnableSystemStatusTopics", this.enableSystemStatusTopics);
    config.put("EnableSystemTopicAverages", this.enableSystemTopicAverages);
    config.put("EnableJMX", this.enableJMX);
    config.put("EnableJMXStatistics", this.enableJMXStatistics);
    config.put("tagMetaData", this.tagMetaData);
    config.put("latitude", this.latitude);
    config.put("longitude", this.longitude);
    return config;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new MessageDaemonConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "MessageDaemon";
  }
}
