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

package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import java.util.Map;
import lombok.Getter;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.SYS_STATUS;

@Getter
public class SysStatusPacket extends MavlinkPacket {

  private final long onboardControlSensorsPresent;
  private final long onboardControlSensorsEnabled;
  private final long onboardControlSensorsHealth;

  private final long onboardControlSensorsPresentExtended;
  private final long onboardControlSensorsEnabledExtended;

  private final double loadPercent;
  private final double voltageVolts;
  private final double currentAmps;
  private final double remainingPercent;

  private final int dropRateComm;
  private final int errorsComm;
  private final int errorsCount1;
  private final int errorsCount2;
  private final int errorsCount3;
  private final int errorsCount4;

  private final boolean valid;

  public SysStatusPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.onboardControlSensorsPresent = getUnsignedInt(fields, "onboard_control_sensors_present");
    this.onboardControlSensorsEnabled = getUnsignedInt(fields, "onboard_control_sensors_enabled");
    this.onboardControlSensorsHealth = getUnsignedInt(fields, "onboard_control_sensors_health");

    this.onboardControlSensorsPresentExtended = getUnsignedInt(fields, "onboard_control_sensors_present_extended");
    this.onboardControlSensorsEnabledExtended = getUnsignedInt(fields, "onboard_control_sensors_enabled_extended");

    this.loadPercent = getLoadPercent(fields);
    this.voltageVolts = getVoltageVolts(fields);
    this.currentAmps = getCurrentAmps(fields);
    this.remainingPercent = getRemainingPercent(fields);

    this.dropRateComm = getInt(fields, "drop_rate_comm");
    this.errorsComm = getInt(fields, "errors_comm");
    this.errorsCount1 = getInt(fields, "errors_count1");
    this.errorsCount2 = getInt(fields, "errors_count2");
    this.errorsCount3 = getInt(fields, "errors_count3");
    this.errorsCount4 = getInt(fields, "errors_count4");

    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return SYS_STATUS;
  }

  public boolean areEnabledSensorsHealthy() {
    return (onboardControlSensorsEnabled & ~onboardControlSensorsHealth) == 0;
  }

  public boolean hasCommunicationErrors() {
    return errorsComm > 0
        || errorsCount1 > 0
        || errorsCount2 > 0
        || errorsCount3 > 0
        || errorsCount4 > 0;
  }

  private double getLoadPercent(Map<String, Object> fields) {
    Object value = fields.get("load");
    if (value == null) {
      return Double.NaN;
    }

    double raw = ((Number) value).doubleValue();
    if (raw < 0) {
      return Double.NaN;
    }

    return raw / 10.0;
  }

  private double getVoltageVolts(Map<String, Object> fields) {
    Object value = fields.get("voltage_battery");
    if (value == null) {
      return Double.NaN;
    }

    double raw = ((Number) value).doubleValue();
    if (raw == 65535) {
      return Double.NaN;
    }

    return raw / 1000.0;
  }

  private double getCurrentAmps(Map<String, Object> fields) {
    Object value = fields.get("current_battery");
    if (value == null) {
      return Double.NaN;
    }

    double raw = ((Number) value).doubleValue();
    if (raw < 0) {
      return Double.NaN;
    }

    return raw / 100.0;
  }

  private double getRemainingPercent(Map<String, Object> fields) {
    Object value = fields.get("battery_remaining");
    if (value == null) {
      return Double.NaN;
    }

    double raw = ((Number) value).doubleValue();
    if (raw < 0) {
      return Double.NaN;
    }

    return raw;
  }

  private long getUnsignedInt(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return 0L;
    }

    return ((Number) value).longValue() & 0xFFFFFFFFL;
  }
}