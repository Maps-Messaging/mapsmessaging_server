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

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MavlinkBootstrapRequestDefinition {

  private MavlinkBootstrapRequestType requestType;
  private int mavlinkMessageId;
  private int intervalMicroseconds;

  public static MavlinkBootstrapRequestDefinition requestMessage(int mavlinkMessageId) {
    MavlinkBootstrapRequestDefinition definition = new MavlinkBootstrapRequestDefinition();
    definition.setRequestType(MavlinkBootstrapRequestType.REQUEST_MESSAGE);
    definition.setMavlinkMessageId(mavlinkMessageId);
    return definition;
  }

  public static MavlinkBootstrapRequestDefinition setMessageInterval(
      int mavlinkMessageId,
      int intervalMicroseconds
  ) {
    MavlinkBootstrapRequestDefinition definition = new MavlinkBootstrapRequestDefinition();
    definition.setRequestType(MavlinkBootstrapRequestType.SET_MESSAGE_INTERVAL);
    definition.setMavlinkMessageId(mavlinkMessageId);
    definition.setIntervalMicroseconds(intervalMicroseconds);
    return definition;
  }
}