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
import io.mapsmessaging.state.drone.model.SystemState;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.SysStatusPacket;

import java.time.Instant;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.SYS_STATUS;

public class SysStatusListener implements Listener {

  public static final int LISTENER_ID = SYS_STATUS;

  private final TwinManager twinManager;

  public SysStatusListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof SysStatusPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    Instant now = (context != null && context.getReceivedTime() != null)
        ? context.getReceivedTime()
        : Instant.now();

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin drone = (DroneTwin) twin;

      updateBatteryState(drone, packet);
      updateSystemState(drone, packet);

      drone.setPowerUpdatedAt(now);
      drone.setOperationalUpdatedAt(now);

    }, context);
  }

  private void updateBatteryState(DroneTwin drone, SysStatusPacket packet) {
    BatteryState batteryState = drone.getBatteryState();
    if (batteryState == null) {
      batteryState = new BatteryState();
    }

    if (!Double.isNaN(packet.getVoltageVolts())) {
      batteryState.setVoltageVolts(packet.getVoltageVolts());
    }

    if (!Double.isNaN(packet.getCurrentAmps())) {
      batteryState.setCurrentAmps(packet.getCurrentAmps());
    }

    if (!Double.isNaN(packet.getRemainingPercent())) {
      batteryState.setPercentage(packet.getRemainingPercent());
    }

    drone.setBatteryState(batteryState);
  }

  private void updateSystemState(DroneTwin drone, SysStatusPacket packet) {
    SystemState systemState = drone.getSystemState();
    if (systemState == null) {
      systemState = new SystemState();
    }

    if (!Double.isNaN(packet.getLoadPercent())) {
      systemState.setCpuLoadPercent(packet.getLoadPercent());
    }

    boolean healthy = packet.getOnboardControlSensorsEnabled() == packet.getOnboardControlSensorsHealth();
    systemState.setHealthy(healthy);
    systemState.setStatusMessage(buildStatusMessage(packet, healthy));

    drone.setSystemState(systemState);
  }

  private String buildStatusMessage(SysStatusPacket packet, boolean healthy) {
    if (healthy) {
      return "System health nominal";
    }

    return "System health degraded";
  }
}