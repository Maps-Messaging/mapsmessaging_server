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

package io.mapsmessaging.state.mavlink.bootstrap;

public enum DroneTwinMissingState {

  MISSING_SYSTEM_ID,
  MISSING_COMPONENT_ID,
  MISSING_VEHICLE_CLASS,
  MISSING_AUTOPILOT_TYPE,
  MISSING_AUTOPILOT_VERSION,
  MISSING_GLOBAL_POSITION,
  MISSING_GPS_FIX,
  MISSING_HOME_POSITION,
  MISSING_BATTERY_STATE,
  MISSING_SYSTEM_STATE,
  MISSING_CAPABILITIES,
  STALE_HEARTBEAT,
  STALE_POSITION,
  STALE_POWER

}