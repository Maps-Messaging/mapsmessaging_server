/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;

public class MemoryConfig extends MemoryConfigDTO implements Config {

  public MemoryConfig(ConfigurationProperties config) {
    setType("memory");
    NetworkConfigFactory.unpack(config, this);
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    return update instanceof MemoryConfigDTO memoryConfigDTO && NetworkConfigFactory.update(this, memoryConfigDTO);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    NetworkConfigFactory.pack(config, this);
    return config;
  }
}
