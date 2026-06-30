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
import java.util.Arrays;
import java.util.Map;
import lombok.Getter;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.BATTERY_STATUS;

/**
 * MAVLink BATTERY_STATUS mapped to a typed packet.
 */
@Getter
public class BatteryStatusPacket extends MavlinkPacket {

  private static final int UNKNOWN_TEMPERATURE = 32767;
  private static final int UNKNOWN_VOLTAGE = 65535;
  private static final int UNKNOWN_CURRENT = -1;
  private static final int UNKNOWN_REMAINING = -1;

  private final int id;
  private final int batteryFunction;
  private final int type;
  private final int temperature;
  private final int[] voltages;
  private final int currentBattery;
  private final int currentConsumed;
  private final int energyConsumed;
  private final int batteryRemaining;

  private final boolean timeRemainingPresent;
  private final int timeRemaining;

  private final boolean chargeStatePresent;
  private final int chargeState;

  private final boolean voltagesExtPresent;
  private final int[] voltagesExt;

  private final boolean modePresent;
  private final int mode;

  private final boolean faultBitmaskPresent;
  private final long faultBitmask;

  private final boolean valid;

  public BatteryStatusPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.id = getInt(fields, "id");
    this.batteryFunction = getInt(fields, "battery_function");
    this.type = getInt(fields, "type");
    this.temperature = getInt(fields, "temperature");
    this.voltages = getIntArray(fields, "voltages");
    this.currentBattery = getInt(fields, "current_battery");
    this.currentConsumed = getInt(fields, "current_consumed");
    this.energyConsumed = getInt(fields, "energy_consumed");
    this.batteryRemaining = getInt(fields, "battery_remaining");

    this.timeRemainingPresent = fields.containsKey("time_remaining");
    this.timeRemaining = getInt(fields, "time_remaining");

    this.chargeStatePresent = fields.containsKey("charge_state");
    this.chargeState = getInt(fields, "charge_state");

    this.voltagesExtPresent = fields.containsKey("voltages_ext");
    this.voltagesExt = getIntArray(fields, "voltages_ext");

    this.modePresent = fields.containsKey("mode");
    this.mode = getInt(fields, "mode");

    this.faultBitmaskPresent = fields.containsKey("fault_bitmask");
    this.faultBitmask = getLong(fields, "fault_bitmask");

    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return BATTERY_STATUS;
  }

  public boolean isTemperatureKnown() {
    return temperature != UNKNOWN_TEMPERATURE;
  }

  public double getTemperatureDegreesCelsius() {
    if (!isTemperatureKnown()) {
      return Double.NaN;
    }
    return temperature / 100.0;
  }

  public boolean isCurrentBatteryKnown() {
    return currentBattery != UNKNOWN_CURRENT;
  }

  public double getCurrentBatteryAmps() {
    if (!isCurrentBatteryKnown()) {
      return Double.NaN;
    }
    return currentBattery / 100.0;
  }

  public boolean isBatteryRemainingKnown() {
    return batteryRemaining != UNKNOWN_REMAINING;
  }

  public boolean hasFaults() {
    return faultBitmaskPresent && faultBitmask != 0;
  }

  public int[] getKnownVoltages() {
    return Arrays.stream(voltages)
        .filter(voltage -> voltage != UNKNOWN_VOLTAGE)
        .toArray();
  }
}