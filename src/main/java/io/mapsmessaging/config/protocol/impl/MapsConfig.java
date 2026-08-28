/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MapsConfigDTO;

public class MapsConfig extends MapsConfigDTO implements Config {

  public MapsConfig() {
  }

  public MapsConfig(ConfigurationProperties config) {
    ProtocolConfigFactory.unpack(config, this);
    keepAlive = config.getIntProperty("keepAlive", keepAlive);
    maximumFrameSize = config.getIntProperty("maximumFrameSize", maximumFrameSize);
    receiveMaximum = config.getIntProperty("receiveMaximum", receiveMaximum);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof MapsConfigDTO updated)) {
      return false;
    }
    boolean changed = ProtocolConfigFactory.update(this, updated);
    if (keepAlive != updated.getKeepAlive()) {
      keepAlive = updated.getKeepAlive();
      changed = true;
    }
    if (maximumFrameSize != updated.getMaximumFrameSize()) {
      maximumFrameSize = updated.getMaximumFrameSize();
      changed = true;
    }
    if (receiveMaximum != updated.getReceiveMaximum()) {
      receiveMaximum = updated.getReceiveMaximum();
      changed = true;
    }
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    ProtocolConfigFactory.pack(config, this);
    config.put("keepAlive", keepAlive);
    config.put("maximumFrameSize", maximumFrameSize);
    config.put("receiveMaximum", receiveMaximum);
    return config;
  }
}
