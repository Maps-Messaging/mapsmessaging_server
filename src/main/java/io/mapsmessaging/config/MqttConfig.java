package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MqttConfig extends Config {
  private long maximumSessionExpiry;
  private long maximumBufferSize;
  private int serverReceiveMaximum;
  private int clientReceiveMaximum;
  private int clientMaximumTopicAlias;
  private int serverMaximumTopicAlias;
  private boolean strictClientId;

  public MqttConfig(ConfigurationProperties config) {
    this.maximumSessionExpiry = config.getLongProperty("maximumSessionExpiry", 86400);
    this.maximumBufferSize = parseBufferSize(config.getProperty("maximumBufferSize", "10M"));
    this.serverReceiveMaximum = config.getIntProperty("serverReceiveMaximum", 10);
    this.clientReceiveMaximum = config.getIntProperty("clientReceiveMaximum", 65535);
    this.clientMaximumTopicAlias = config.getIntProperty("clientMaximumTopicAlias", 32767);
    this.serverMaximumTopicAlias = config.getIntProperty("serverMaximumTopicAlias", 0);
    this.strictClientId = config.getBooleanProperty("strictClientId", false);
  }

  public boolean update(MqttConfig newConfig) {
    boolean hasChanged = false;

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

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
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
