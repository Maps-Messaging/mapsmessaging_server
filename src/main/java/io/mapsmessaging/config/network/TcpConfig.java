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

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class TcpConfig extends EndPointConfig {

  private int receiveBufferSize;
  private int sendBufferSize;
  private int timeout;
  private int backlog;
  private int soLingerDelaySec;
  private int readDelayOnFragmentation;
  private boolean enableReadDelayOnFragmentation;
  private long serverReadBufferSize;
  private long serverWriteBufferSize;
  private int selectorThreadCount;

  public TcpConfig(ConfigurationProperties config) {
    super(config);
    this.receiveBufferSize = config.getIntProperty("receiveBufferSize", 128000);
    this.sendBufferSize = config.getIntProperty("sendBufferSize", 128000);
    this.timeout = config.getIntProperty("timeout", 60000);
    this.backlog = config.getIntProperty("backlog", 100);
    this.soLingerDelaySec = config.getIntProperty("soLingerDelaySec", 10);
    this.readDelayOnFragmentation = config.getIntProperty("readDelayOnFragmentation", 100);
    this.enableReadDelayOnFragmentation = config.getBooleanProperty("enableReadDelayOnFragmentation", true);
    this.serverReadBufferSize = parseBufferSize(config.getProperty("serverReadBufferSize", "10K"));
    this.serverWriteBufferSize = parseBufferSize(config.getProperty("serverWriteBufferSize", "10K"));
    this.selectorThreadCount = config.getIntProperty("selectorThreadCount", 2);
  }

  public boolean update(TcpConfig newConfig) {
    boolean hasChanged = super.update(newConfig);

    if (this.receiveBufferSize != newConfig.getReceiveBufferSize()) {
      this.receiveBufferSize = newConfig.getReceiveBufferSize();
      hasChanged = true;
    }
    if (this.sendBufferSize != newConfig.getSendBufferSize()) {
      this.sendBufferSize = newConfig.getSendBufferSize();
      hasChanged = true;
    }
    if (this.timeout != newConfig.getTimeout()) {
      this.timeout = newConfig.getTimeout();
      hasChanged = true;
    }
    if (this.backlog != newConfig.getBacklog()) {
      this.backlog = newConfig.getBacklog();
      hasChanged = true;
    }
    if (this.soLingerDelaySec != newConfig.getSoLingerDelaySec()) {
      this.soLingerDelaySec = newConfig.getSoLingerDelaySec();
      hasChanged = true;
    }
    if (this.readDelayOnFragmentation != newConfig.getReadDelayOnFragmentation()) {
      this.readDelayOnFragmentation = newConfig.getReadDelayOnFragmentation();
      hasChanged = true;
    }
    if (this.enableReadDelayOnFragmentation != newConfig.isEnableReadDelayOnFragmentation()) {
      this.enableReadDelayOnFragmentation = newConfig.isEnableReadDelayOnFragmentation();
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
    if (this.selectorThreadCount != newConfig.getSelectorThreadCount()) {
      this.selectorThreadCount = newConfig.getSelectorThreadCount();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = super.toConfigurationProperties();
    config.put("receiveBufferSize", this.receiveBufferSize);
    config.put("sendBufferSize", this.sendBufferSize);
    config.put("timeout", this.timeout);
    config.put("backlog", this.backlog);
    config.put("soLingerDelaySec", this.soLingerDelaySec);
    config.put("readDelayOnFragmentation", this.readDelayOnFragmentation);
    config.put("enableReadDelayOnFragmentation", this.enableReadDelayOnFragmentation);
    config.put("serverReadBufferSize", formatBufferSize(this.serverReadBufferSize));
    config.put("serverWriteBufferSize", formatBufferSize(this.serverWriteBufferSize));
    config.put("selectorThreadCount", this.selectorThreadCount);
    return config;
  }
}