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
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechTransmitDefaultsDTO;

public class SemtechConfig extends SemtechConfigDTO implements Config {

  public SemtechConfig(ConfigurationProperties config) {
    setType("semtech");
    ProtocolConfigFactory.unpack(config, this);

    this.maxQueued = config.getIntProperty("MaxQueueSize", maxQueued);
    this.inboundTopicName = config.getProperty("inbound", inboundTopicName);
    this.outboundTopicName = config.getProperty("outbound", outboundTopicName);
    this.statusTopicName = config.getProperty("status", statusTopicName);
    this.telemetryTopicName = config.getProperty("telemetry", telemetryTopicName);
    unpackTransmitDefaults((ConfigurationProperties) config.get("tx"));
  }

  private void unpackTransmitDefaults(ConfigurationProperties config) {
    if (this.transmitDefaults == null) {
      this.transmitDefaults = new SemtechTransmitDefaultsDTO();
    }
    if(config == null) config = new ConfigurationProperties();
    this.transmitDefaults.setImme(config.getBooleanProperty("imme", this.transmitDefaults.isImme()));
    this.transmitDefaults.setFreq(config.getDoubleProperty("freq", this.transmitDefaults.getFreq()));
    this.transmitDefaults.setRfch(config.getIntProperty("rfch", this.transmitDefaults.getRfch()));
    this.transmitDefaults.setPowe(config.getIntProperty("powe", this.transmitDefaults.getPowe()));
    this.transmitDefaults.setModu(config.getProperty("modu", this.transmitDefaults.getModu()));
    this.transmitDefaults.setDatr(config.getProperty("datr", this.transmitDefaults.getDatr()));
    this.transmitDefaults.setCodr(config.getProperty("codr", this.transmitDefaults.getCodr()));
    this.transmitDefaults.setIpol(config.getBooleanProperty("ipol", this.transmitDefaults.isIpol()));
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof SemtechConfigDTO newConfig) {

      if (this.maxQueued != newConfig.getMaxQueued()) {
        this.maxQueued = newConfig.getMaxQueued();
        hasChanged = true;
      }

      if (!this.inboundTopicName.equals(newConfig.getInboundTopicName())) {
        this.inboundTopicName = newConfig.getInboundTopicName();
        hasChanged = true;
      }

      if (!this.outboundTopicName.equals(newConfig.getOutboundTopicName())) {
        this.outboundTopicName = newConfig.getOutboundTopicName();
        hasChanged = true;
      }

      if (!this.statusTopicName.equals(newConfig.getStatusTopicName())) {
        this.statusTopicName = newConfig.getStatusTopicName();
        hasChanged = true;
      }

      if (!this.telemetryTopicName.equals(newConfig.getTelemetryTopicName())) {
        this.telemetryTopicName = newConfig.getTelemetryTopicName();
        hasChanged = true;
      }

      if (updateTransmitDefaults(newConfig.getTransmitDefaults())) {
        hasChanged = true;
      }

      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }

    return hasChanged;
  }

  private boolean updateTransmitDefaults(SemtechTransmitDefaultsDTO newDefaults) {
    if (newDefaults == null) {
      return false;
    }

    if (this.transmitDefaults == null) {
      this.transmitDefaults = new SemtechTransmitDefaultsDTO();
    }

    boolean hasChanged = false;

    if (this.transmitDefaults.isImme() != newDefaults.isImme()) {
      this.transmitDefaults.setImme(newDefaults.isImme());
      hasChanged = true;
    }

    if (Double.compare(this.transmitDefaults.getFreq(), newDefaults.getFreq()) != 0) {
      this.transmitDefaults.setFreq(newDefaults.getFreq());
      hasChanged = true;
    }

    if (this.transmitDefaults.getRfch() != newDefaults.getRfch()) {
      this.transmitDefaults.setRfch(newDefaults.getRfch());
      hasChanged = true;
    }

    if (this.transmitDefaults.getPowe() != newDefaults.getPowe()) {
      this.transmitDefaults.setPowe(newDefaults.getPowe());
      hasChanged = true;
    }

    if (!safeEquals(this.transmitDefaults.getModu(), newDefaults.getModu())) {
      this.transmitDefaults.setModu(newDefaults.getModu());
      hasChanged = true;
    }

    if (!safeEquals(this.transmitDefaults.getDatr(), newDefaults.getDatr())) {
      this.transmitDefaults.setDatr(newDefaults.getDatr());
      hasChanged = true;
    }

    if (!safeEquals(this.transmitDefaults.getCodr(), newDefaults.getCodr())) {
      this.transmitDefaults.setCodr(newDefaults.getCodr());
      hasChanged = true;
    }

    if (this.transmitDefaults.isIpol() != newDefaults.isIpol()) {
      this.transmitDefaults.setIpol(newDefaults.isIpol());
      hasChanged = true;
    }

    return hasChanged;
  }

  private boolean safeEquals(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null) {
      return false;
    }
    return left.equals(right);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();

    ProtocolConfigFactory.pack(properties, this);

    properties.put("MaxQueueSize", this.maxQueued);
    properties.put("inbound", this.inboundTopicName);
    properties.put("outbound", this.outboundTopicName);
    properties.put("status", this.statusTopicName);
    properties.put("telemetry", this.telemetryTopicName);

    packTransmitDefaults(properties);

    return properties;
  }

  private void packTransmitDefaults(ConfigurationProperties properties) {
    if (this.transmitDefaults == null) {
      return;
    }
    ConfigurationProperties tx = new ConfigurationProperties();
    tx.put("imme", Boolean.toString(this.transmitDefaults.isImme()));
    tx.put("freq", Double.toString(this.transmitDefaults.getFreq()));
    tx.put("rfch", Integer.toString(this.transmitDefaults.getRfch()));
    tx.put("powe", Integer.toString(this.transmitDefaults.getPowe()));
    tx.put("modu", this.transmitDefaults.getModu());
    tx.put("datr", this.transmitDefaults.getDatr());
    tx.put("codr", this.transmitDefaults.getCodr());
    tx.put("ipol", Boolean.toString(this.transmitDefaults.isIpol()));
    properties.put("tx", tx);
  }
}