/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotPresenceConfigDTO;
import java.util.Objects;

public class CotPresenceConfig extends CotPresenceConfigDTO implements Config {

  public CotPresenceConfig(ConfigurationProperties config) {
    enabled = config.getBooleanProperty("enabled", enabled);
    uid = config.getProperty("uid", uid);
    callsign = config.getProperty("callsign", callsign);
    cotType = config.getProperty("cotType", cotType);
    how = config.getProperty("how", how);
    latitude = config.getDoubleProperty("latitude", latitude);
    longitude = config.getDoubleProperty("longitude", longitude);
    hae = config.getDoubleProperty("hae", hae);
    ce = config.getDoubleProperty("ce", ce);
    le = config.getDoubleProperty("le", le);
    groupName = config.getProperty("groupName", groupName);
    groupRole = config.getProperty("groupRole", groupRole);
    device = config.getProperty("device", device);
    platform = config.getProperty("platform", platform);
    operatingSystem = config.getProperty("operatingSystem", operatingSystem);
    softwareVersion = config.getProperty("softwareVersion", softwareVersion);
    intervalSeconds = config.getIntProperty("intervalSeconds", intervalSeconds);
    staleSeconds = config.getIntProperty("staleSeconds", staleSeconds);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof CotPresenceConfigDTO updated)) {
      return false;
    }
    boolean changed = false;
    changed |= updateBoolean(enabled, updated.isEnabled(), value -> enabled = value);
    changed |= updateString(uid, updated.getUid(), value -> uid = value);
    changed |= updateString(callsign, updated.getCallsign(), value -> callsign = value);
    changed |= updateString(cotType, updated.getCotType(), value -> cotType = value);
    changed |= updateString(how, updated.getHow(), value -> how = value);
    changed |= updateDouble(latitude, updated.getLatitude(), value -> latitude = value);
    changed |= updateDouble(longitude, updated.getLongitude(), value -> longitude = value);
    changed |= updateDouble(hae, updated.getHae(), value -> hae = value);
    changed |= updateDouble(ce, updated.getCe(), value -> ce = value);
    changed |= updateDouble(le, updated.getLe(), value -> le = value);
    changed |= updateString(groupName, updated.getGroupName(), value -> groupName = value);
    changed |= updateString(groupRole, updated.getGroupRole(), value -> groupRole = value);
    changed |= updateString(device, updated.getDevice(), value -> device = value);
    changed |= updateString(platform, updated.getPlatform(), value -> platform = value);
    changed |= updateString(operatingSystem, updated.getOperatingSystem(), value -> operatingSystem = value);
    changed |= updateString(softwareVersion, updated.getSoftwareVersion(), value -> softwareVersion = value);
    changed |= updateInt(intervalSeconds, updated.getIntervalSeconds(), value -> intervalSeconds = value);
    changed |= updateInt(staleSeconds, updated.getStaleSeconds(), value -> staleSeconds = value);
    return changed;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", enabled);
    properties.put("uid", uid);
    properties.put("callsign", callsign);
    properties.put("cotType", cotType);
    properties.put("how", how);
    properties.put("latitude", latitude);
    properties.put("longitude", longitude);
    properties.put("hae", hae);
    properties.put("ce", ce);
    properties.put("le", le);
    properties.put("groupName", groupName);
    properties.put("groupRole", groupRole);
    properties.put("device", device);
    properties.put("platform", platform);
    properties.put("operatingSystem", operatingSystem);
    properties.put("softwareVersion", softwareVersion);
    properties.put("intervalSeconds", intervalSeconds);
    properties.put("staleSeconds", staleSeconds);
    return properties;
  }

  private static boolean updateString(String current, String updated, java.util.function.Consumer<String> setter) {
    if (Objects.equals(current, updated)) {
      return false;
    }
    setter.accept(updated);
    return true;
  }

  private static boolean updateBoolean(boolean current, boolean updated, java.util.function.Consumer<Boolean> setter) {
    if (current == updated) {
      return false;
    }
    setter.accept(updated);
    return true;
  }

  private static boolean updateDouble(double current, double updated, java.util.function.DoubleConsumer setter) {
    if (Double.compare(current, updated) == 0) {
      return false;
    }
    setter.accept(updated);
    return true;
  }

  private static boolean updateInt(int current, int updated, java.util.function.IntConsumer setter) {
    if (current == updated) {
      return false;
    }
    setter.accept(updated);
    return true;
  }
}
