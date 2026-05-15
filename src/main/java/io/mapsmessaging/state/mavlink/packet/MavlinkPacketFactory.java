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

public class MavlinkPacketFactory {

  public static MavlinkPacket create(ProcessedFrame frame) {

    int messageId = frame.getFrame().getMessageId();

    return switch (messageId) {
      case MavlinkMessageIds.GLOBAL_POSITION_INT -> new GlobalPositionPacket(frame);
      case MavlinkMessageIds.ATTITUDE -> new AttitudePacket(frame);
      case MavlinkMessageIds.GPS_RAW_INT -> new GpsRawIntPacket(frame);
      case MavlinkMessageIds.HEARTBEAT -> new HeartbeatPacket(frame);
      case MavlinkMessageIds.SYS_STATUS -> new SysStatusPacket(frame);
      case MavlinkMessageIds.SYSTEM_TIME -> new SystemTimePacket(frame);
      case MavlinkMessageIds.ALTITUDE -> new AltitudePacket(frame);
      case MavlinkMessageIds.EXTENDED_SYS_STATE -> new ExtendedSysStatePacket(frame);
      case MavlinkMessageIds.MISSION_CURRENT -> new MissionCurrentPacket(frame);
      case MavlinkMessageIds.AUTOPILOT_VERSION -> new AutopilotVersionPacket(frame);
      case MavlinkMessageIds.HOME_POSITION -> new HomePositionPacket(frame);
      default -> null;
    };
  }
}