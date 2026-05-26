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

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.BatteryState;
import io.mapsmessaging.state.mavlink.packet.BatteryStatusPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;

import java.time.Instant;
import java.util.Arrays;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.BATTERY_STATUS;

/**
 * Listener for BATTERY_STATUS.
 */
public class BatteryStatusListener implements Listener {

  public static final int LISTENER_ID = BATTERY_STATUS;

  private final TwinManager twinManager;

  public BatteryStatusListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof BatteryStatusPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin droneTwin = (DroneTwin) twin;

      BatteryState batteryState = droneTwin.getBatteryState();
      if (batteryState == null) {
        batteryState = new BatteryState();
      }

      if (packet.isBatteryRemainingKnown()) {
        batteryState.setPercentage((double) packet.getBatteryRemaining());
      }

      if (packet.isCurrentBatteryKnown()) {
        batteryState.setCurrentAmps(packet.getCurrentBatteryAmps());
      }

      if (packet.isTemperatureKnown()) {
        batteryState.setTemperatureCelsius(packet.getTemperatureDegreesCelsius());
      }

      double voltageVolts = calculateVoltageVolts(packet);
      if (Double.isFinite(voltageVolts)) {
        batteryState.setVoltageVolts(voltageVolts);
      }

      if (packet.getCurrentConsumed() >= 0 && packet.isBatteryRemainingKnown()) {
        double consumedMilliampHours = packet.getCurrentConsumed();
        double remainingFraction = packet.getBatteryRemaining() / 100.0;
        if (remainingFraction > 0.0) {
          double estimatedCapacityMilliampHours = consumedMilliampHours / (1.0 - remainingFraction);
          double remainingMilliampHours = estimatedCapacityMilliampHours * remainingFraction;
          batteryState.setRemainingMilliampHours(remainingMilliampHours);
        }
      }

      batteryState.setCharging(isCharging(packet));

      droneTwin.setBatteryState(batteryState);

      Instant now = (context != null && context.getReceivedTime() != null)
          ? context.getReceivedTime()
          : Instant.now();

      droneTwin.setOperationalUpdatedAt(now);

    }, context);
  }

  private double calculateVoltageVolts(BatteryStatusPacket packet) {
    int[] knownVoltages = packet.getKnownVoltages();
    if (knownVoltages.length == 0) {
      return Double.NaN;
    }

    int totalMillivolts = Arrays.stream(knownVoltages).sum();
    return totalMillivolts / 1000.0;
  }

  private boolean isCharging(BatteryStatusPacket packet) {
    if (!packet.isChargeStatePresent()) {
      return false;
    }

    return packet.getChargeState() == 2
        || packet.getChargeState() == 3;
  }
}