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

package io.mapsmessaging.network.protocol.impl.mavlink.packet;

public class MavlinkMessageIds {

  public static final int HEARTBEAT = 0;
  public static final int SYS_STATUS = 1;
  public static final int SYSTEM_TIME = 2;
  public static final int GPS_RAW_INT = 24;
  public static final int ATTITUDE = 30;
  public static final int GLOBAL_POSITION_INT = 33;
  public static final int MISSION_CURRENT = 42;
  public static final int ALTITUDE = 141;
  public static final int AUTOPILOT_VERSION = 148;
  public static final int HOME_POSITION = 242;
  public static final int EXTENDED_SYS_STATE = 245;

  private MavlinkMessageIds() {
  }
}
