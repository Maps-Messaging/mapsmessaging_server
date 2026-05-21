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

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.HOME_POSITION;

@Getter
public class HomePositionPacket extends MavlinkPacket {

  private final int latitudeRaw;
  private final int longitudeRaw;
  private final int altitudeRaw;
  private final boolean valid;

  public HomePositionPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.latitudeRaw = getInt(fields, "latitude");
    this.longitudeRaw = getInt(fields, "longitude");
    this.altitudeRaw = getInt(fields, "altitude");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return HOME_POSITION;
  }

  public double getLatitude() {
    if (latitudeRaw == Integer.MIN_VALUE || latitudeRaw == -1) {
      return Double.NaN;
    }
    return latitudeRaw / 10_000_000.0;
  }

  public double getLongitude() {
    if (longitudeRaw == Integer.MIN_VALUE || longitudeRaw == -1) {
      return Double.NaN;
    }
    return longitudeRaw / 10_000_000.0;
  }

  public double getAltitudeMeters() {
    if (altitudeRaw == Integer.MIN_VALUE || altitudeRaw == -1) {
      return Double.NaN;
    }
    return altitudeRaw / 1000.0;
  }
}