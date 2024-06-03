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

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class MqttConfig extends ProtocolConfig {

  private long maximumSessionExpiry;
  private long maximumBufferSize;
  private int serverReceiveMaximum;
  private int clientReceiveMaximum;
  private int clientMaximumTopicAlias;
  private int serverMaximumTopicAlias;
  private boolean strictClientId;

  public MqttConfig(ConfigurationProperties config) {
    super(config);
    this.maximumSessionExpiry = config.getLongProperty("maximumSessionExpiry", 86400);
    this.maximumBufferSize = parseBufferSize(config.getProperty("maximumBufferSize", "10M"));
    this.serverReceiveMaximum = config.getIntProperty("serverReceiveMaximum", 10);
    this.clientReceiveMaximum = config.getIntProperty("clientReceiveMaximum", 65535);
    this.clientMaximumTopicAlias = config.getIntProperty("clientMaximumTopicAlias", 32767);
    this.serverMaximumTopicAlias = config.getIntProperty("serverMaximumTopicAlias", 0);
    this.strictClientId = config.getBooleanProperty("strictClientId", false);
  }

  public boolean update(MqttConfig newConfig) {
    boolean hasChanged = super.update(newConfig);

    if (this.maximumSessionExpiry != newConfig.getMaximumSessionExpiry()) {
      this.maximumSessionExpiry = newConfig.getMaximumSessionExpiry();
      hasChanged = true;
    }
    if (this.maximumBufferSize != newConfig.getMaximumBufferSize()) {
      this.maximumBufferSize = newConfig.getMaximumBufferSize();
      hasChanged = true;
    }
    if (this.serverReceiveMaximum != newConfig.getServerReceiveMaximum()) {
      this.serverReceiveMaximum = newConfig.getServerReceiveMaximum();
      hasChanged = true;
    }
    if (this.clientReceiveMaximum != newConfig.getClientReceiveMaximum()) {
      this.clientReceiveMaximum = newConfig.getClientReceiveMaximum();
      hasChanged = true;
    }
    if (this.clientMaximumTopicAlias != newConfig.getClientMaximumTopicAlias()) {
      this.clientMaximumTopicAlias = newConfig.getClientMaximumTopicAlias();
      hasChanged = true;
    }
    if (this.serverMaximumTopicAlias != newConfig.getServerMaximumTopicAlias()) {
      this.serverMaximumTopicAlias = newConfig.getServerMaximumTopicAlias();
      hasChanged = true;
    }
    if (this.strictClientId != newConfig.isStrictClientId()) {
      this.strictClientId = newConfig.isStrictClientId();
      hasChanged = true;
    }

    return hasChanged;
  }
  @Override
  public String getType() {
    return "mqtt";
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = super.toConfigurationProperties();
    config.put("maximumSessionExpiry", this.maximumSessionExpiry);
    config.put("maximumBufferSize", formatBufferSize(this.maximumBufferSize));
    config.put("serverReceiveMaximum", this.serverReceiveMaximum);
    config.put("clientReceiveMaximum", this.clientReceiveMaximum);
    config.put("clientMaximumTopicAlias", this.clientMaximumTopicAlias);
    config.put("serverMaximumTopicAlias", this.serverMaximumTopicAlias);
    config.put("strictClientId", this.strictClientId);
    return config;
  }
}
