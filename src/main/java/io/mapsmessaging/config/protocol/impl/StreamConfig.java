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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StreamConfigDTO;
import io.mapsmessaging.network.protocol.impl.stream.StreamAssemblyType;
import io.mapsmessaging.network.protocol.impl.stream.assemblers.LengthEndian;

public class StreamConfig extends StreamConfigDTO implements Config {

  public StreamConfig(ConfigurationProperties config) {
    setType("stream");
    ProtocolConfigFactory.unpack(config, this);

    this.maxBufferSize = config.getIntProperty("maximumBufferSize", maxBufferSize);
    this.maxReceive = config.getIntProperty("maximumReceive", maxReceive);

    this.assemblyType = parseAssemblyType(
        config.getProperty("assemblyType"),
        this.assemblyType
    );

    this.schemaName = config.getProperty("schemaName", schemaName);

    this.lengthFieldSize = config.getIntProperty("lengthFieldSize", lengthFieldSize);

    this.endianness = parseEndianness(
        config.getProperty("endianness"),
        this.endianness
    );
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof StreamConfigDTO newConfig) {
      if (this.maxBufferSize != newConfig.getMaxBufferSize()) {
        this.maxBufferSize = newConfig.getMaxBufferSize();
        hasChanged = true;
      }
      if (this.maxReceive != newConfig.getMaxReceive()) {
        this.maxReceive = newConfig.getMaxReceive();
        hasChanged = true;
      }
      if (this.assemblyType != newConfig.getAssemblyType()) {
        this.assemblyType = newConfig.getAssemblyType();
        hasChanged = true;
      }
      if (this.lengthFieldSize != newConfig.getLengthFieldSize()) {
        this.lengthFieldSize = newConfig.getLengthFieldSize();
        hasChanged = true;
      }
      if (this.endianness != newConfig.getEndianness()) {
        this.endianness = newConfig.getEndianness();
        hasChanged = true;
      }
      if (!this.schemaName.equals(newConfig.getSchemaName())) {
        this.schemaName = newConfig.getSchemaName();
        hasChanged = true;
      }
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("maximumBufferSize", this.maxBufferSize);
    properties.put("maximumReceive", this.maxReceive);
    properties.put("assemblyType", this.assemblyType.name());
    properties.put("lengthFieldSize", this.lengthFieldSize);
    properties.put("endianness", this.endianness.name());
    properties.put("schemaName", this.schemaName);
    return properties;
  }

  private StreamAssemblyType parseAssemblyType(Object value, StreamAssemblyType defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    return StreamAssemblyType.valueOf(value.toString().trim().toUpperCase());
  }

  private LengthEndian parseEndianness(Object value, LengthEndian defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    return LengthEndian.valueOf(value.toString().trim().toUpperCase());
  }
}