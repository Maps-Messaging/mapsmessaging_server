package io.mapsmessaging.config.destination;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MessageOverrideConfig extends MessageOverrideDTO implements Config {

  public MessageOverrideConfig(ConfigurationProperties properties) {
    this.expiry = properties.getLongProperty("expiry", -1);
    this.priority = Priority.getInstance(properties.getIntProperty("priority", Priority.NORMAL.getValue()));
    String qos = properties.getProperty("qos", null);
    if(qos != null) {
      this.qualityOfService= QualityOfService.valueOf(qos);
    }
    this.responseTopic = properties.getProperty("responseTopic", null);
    this.contentType = properties.getProperty("contentType", null);
    this.schemaId = properties.getProperty("schemaId", null);
    if(properties.containsKey("storeOffline")) {
      this.storeOffline = properties.getBooleanProperty("storeOffline",false);
    }
    else{
      this.storeOffline = null;
    }
    Map<String, Object> preMeta = ((ConfigurationProperties)properties.get("meta")).getMap();
    if(preMeta != null) {
      meta = new LinkedHashMap<>();
      for(Map.Entry<String, Object> entry : preMeta.entrySet()) {
        meta.put(entry.getKey(), entry.getValue().toString());
      }
    }
    this.dataMap = ((ConfigurationProperties)properties.get("dataMap")).getMap();
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("expiry", this.expiry);
    properties.put("priority", this.priority);
    properties.put("qualityOfService", this.qualityOfService);
    properties.put("responseTopic", this.responseTopic);
    properties.put("contentType", this.contentType);
    properties.put("schemaId", this.schemaId);
    properties.put("storeOffline", this.storeOffline);
    properties.put("meta", this.meta);
    properties.put("dataMap", this.dataMap);
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof MessageOverrideDTO)) {
      return false;
    }

    MessageOverrideDTO newConfig = (MessageOverrideDTO) config;
    boolean hasChanged = false;

    if (!Objects.equals(this.expiry, newConfig.getExpiry())) {
      this.expiry = newConfig.getExpiry();
      hasChanged = true;
    }
    if (!Objects.equals(this.priority, newConfig.getPriority())) {
      this.priority = newConfig.getPriority();
      hasChanged = true;
    }
    if (!Objects.equals(this.qualityOfService, newConfig.getQualityOfService())) {
      this.qualityOfService = newConfig.getQualityOfService();
      hasChanged = true;
    }
    if (!Objects.equals(this.responseTopic, newConfig.getResponseTopic())) {
      this.responseTopic = newConfig.getResponseTopic();
      hasChanged = true;
    }
    if (!Objects.equals(this.contentType, newConfig.getContentType())) {
      this.contentType = newConfig.getContentType();
      hasChanged = true;
    }
    if (!Objects.equals(this.schemaId, newConfig.getSchemaId())) {
      this.schemaId = newConfig.getSchemaId();
      hasChanged = true;
    }
    if (!Objects.equals(this.storeOffline, newConfig.getStoreOffline())) {
      this.storeOffline = newConfig.getStoreOffline();
      hasChanged = true;
    }
    if (!Objects.equals(this.meta, newConfig.getMeta())) {
      this.meta = newConfig.getMeta();
      hasChanged = true;
    }
    if (!Objects.equals(this.dataMap, newConfig.getDataMap())) {
      this.dataMap = newConfig.getDataMap();
      hasChanged = true;
    }

    return hasChanged;
  }
}
