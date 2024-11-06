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

package io.mapsmessaging.config.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.network.impl.*;
import io.mapsmessaging.config.protocol.impl.*;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)

@JsonSubTypes({
    @JsonSubTypes.Type(value = DtlsConfig.class, name = "dtls"),
    @JsonSubTypes.Type(value = LoRaDeviceConfig.class, name = "lora"),
    @JsonSubTypes.Type(value = SerialConfig.class, name = "serial"),
    @JsonSubTypes.Type(value = TcpConfig.class, name = "tcp"),
    @JsonSubTypes.Type(value = TlsConfig.class, name = "ssl"),
    @JsonSubTypes.Type(value = UdpConfig.class, name = "udp"),
})
@Schema(
    description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "dtls", schema = DtlsConfig.class),
        @DiscriminatorMapping(value = "lora", schema = LoRaDeviceConfig.class),
        @DiscriminatorMapping(value = "serial", schema = SerialConfig.class),
        @DiscriminatorMapping(value = "tcp", schema = TcpConfig.class),
        @DiscriminatorMapping(value = "ssl", schema = TlsConfig.class),
        @DiscriminatorMapping(value = "udp", schema = UdpConfig.class),
    })

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class EndPointConfig extends Config {

  private String type;
  private boolean discoverable;
  private int selectorThreadCount;
  private long serverReadBufferSize;
  private long serverWriteBufferSize;

  public EndPointConfig(ConfigurationProperties config) {
    this.selectorThreadCount = config.getIntProperty("selectorThreadCount", 2);
    this.discoverable = config.getBooleanProperty("discoverable", false);
    this.serverReadBufferSize = parseBufferSize(config.getProperty("serverReadBufferSize", "10K"));
    this.serverWriteBufferSize =
        parseBufferSize(config.getProperty("serverWriteBufferSize", "10K"));
  }

  public boolean update(EndPointConfig newConfig) {
    boolean hasChanged = false;
    if (this.selectorThreadCount != newConfig.getSelectorThreadCount()) {
      this.selectorThreadCount = newConfig.getSelectorThreadCount();
      hasChanged = true;
    }
    if (this.discoverable != newConfig.isDiscoverable()) {
      this.discoverable = newConfig.isDiscoverable();
      hasChanged = true;
    }
    if (this.serverReadBufferSize != newConfig.getServerReadBufferSize()) {
      this.serverReadBufferSize = newConfig.getServerReadBufferSize();
      hasChanged = true;
    }
    if (this.serverWriteBufferSize != newConfig.getServerWriteBufferSize()) {
      this.serverWriteBufferSize = newConfig.getServerWriteBufferSize();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("selectorThreadCount", this.selectorThreadCount);
    config.put("discoverable", this.discoverable);
    config.put("serverReadBufferSize", formatBufferSize(this.serverReadBufferSize));
    config.put("serverWriteBufferSize", formatBufferSize(this.serverWriteBufferSize));
    return config;
  }
}
