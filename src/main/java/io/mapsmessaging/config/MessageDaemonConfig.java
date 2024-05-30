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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Message Daemon Configuration")
public class MessageDaemonConfig extends Config {

  private int delayedPublishInterval;
  private int sessionPipeLines;
  private long transactionExpiry;
  private long transactionScan;
  private String compressionName;
  private int compressMessageMinSize;
  private boolean enableResourceStatistics;
  private boolean enableSystemTopics;
  private boolean enableSystemStatusTopics;
  private boolean enableSystemTopicAverages;
  private boolean enableJMX;
  private boolean enableJMXStatistics;
  private boolean tagMetaData;
  private double latitude;
  private double longitude;


  public static MessageDaemonConfig getInstance(){
    return new MessageDaemonConfig(ConfigurationManager.getInstance().getProperties("MessageDaemon"));
  }

  // Constructor to load properties from ConfigurationProperties
  private MessageDaemonConfig(ConfigurationProperties config) {
    this.delayedPublishInterval = config.getIntProperty("DelayedPublishInterval", 1000);
    this.sessionPipeLines = config.getIntProperty("SessionPipeLines", 48);
    this.transactionExpiry = config.getIntProperty("TransactionExpiry", 3600000);
    this.transactionScan = config.getLongProperty("TransactionScan", 5000);
    this.compressionName = config.getProperty("CompressionName", "None");
    this.compressMessageMinSize = config.getIntProperty("CompressMessageMinSize", 1024);
    this.enableResourceStatistics = config.getBooleanProperty("EnableResourceStatistics", false);
    this.enableSystemTopics = config.getBooleanProperty("EnableSystemTopics", true);
    this.enableSystemStatusTopics = config.getBooleanProperty("EnableSystemStatusTopics", true);
    this.enableSystemTopicAverages = config.getBooleanProperty("EnableSystemTopicAverages", false);
    this.enableJMX = config.getBooleanProperty("EnableJMX", true);
    this.enableJMXStatistics = config.getBooleanProperty("EnableJMXStatistics", true);
    this.tagMetaData = config.getBooleanProperty("tagMetaData", false);
    this.latitude = config.getDoubleProperty("latitude", 0.0);
    this.longitude = config.getDoubleProperty("longitude", 0.0);
  }

  // Method to update properties from another MessageDaemonConfig object
  public boolean update(MessageDaemonConfig newConfig) {
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

  // Method to push current values into a ConfigurationProperties object
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
}
