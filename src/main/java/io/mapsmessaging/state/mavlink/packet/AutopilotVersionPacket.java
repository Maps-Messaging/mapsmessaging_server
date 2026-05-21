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
import lombok.Getter;

import java.util.Map;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.AUTOPILOT_VERSION;

@Getter
public class AutopilotVersionPacket extends MavlinkPacket {

  private final long capabilities;
  private final long flightSoftwareVersion;
  private final long middlewareSoftwareVersion;
  private final long osSoftwareVersion;
  private final long boardVersion;
  private final long vendorId;
  private final long productId;
  private final long uid;
  private final String uid2;
  private final boolean valid;

  public AutopilotVersionPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.capabilities = getLong(fields, "capabilities");
    this.flightSoftwareVersion = getLong(fields, "flight_sw_version");
    this.middlewareSoftwareVersion = getLong(fields, "middleware_sw_version");
    this.osSoftwareVersion = getLong(fields, "os_sw_version");
    this.boardVersion = getLong(fields, "board_version");
    this.vendorId = getLong(fields, "vendor_id");
    this.productId = getLong(fields, "product_id");
    this.uid = getLong(fields, "uid");
    this.uid2 = getString(fields, "uid2");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return AUTOPILOT_VERSION;
  }

}