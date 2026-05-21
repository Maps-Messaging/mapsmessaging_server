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


import io.mapsmessaging.mavlink.ProcessedFrame;
import lombok.Getter;

import java.util.Map;

/**
 * MAVLink GLOBAL_POSITION_INT mapped to a typed packet.
 */
@Getter
public final class GlobalPositionPacket extends MavlinkPacket {

  private final double latitude;
  private final double longitude;
  private final double altitudeMeters;

  private final double vx;
  private final double vy;
  private final double vz;

  private final double headingDegrees;

  private final boolean valid;

  public GlobalPositionPacket(ProcessedFrame frame) {

    Map<String, Object> fields = frame.getFields();

    this.latitude = getDouble(fields, "lat") / 1e7;
    this.longitude = getDouble(fields, "lon") / 1e7;
    this.altitudeMeters = getDouble(fields, "alt") / 1000.0;

    this.vx = getDouble(fields, "vx") / 100.0;
    this.vy = getDouble(fields, "vy") / 100.0;
    this.vz = getDouble(fields, "vz") / 100.0;

    this.headingDegrees = getDouble(fields, "hdg") / 100.0;

    this.valid = frame.isValid();
  }

}