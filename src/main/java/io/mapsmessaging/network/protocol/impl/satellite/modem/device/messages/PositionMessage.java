/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public class PositionMessage implements ModemMessage {

  private final int fixStatus;
  private final double latitude;     // 24-bit signed, 0.001 minutes
  private final double longitude;    // 25-bit signed, 0.001 minutes
  private final int altitude;     // 15-bit signed, meters
  private final int speed;        // 8-bit unsigned
  private final int heading;      // 8-bit unsigned (2° increments)
  private final int dayOfMonth;   // 5-bit
  private final int minuteOfDay;  // 11-bit

  public PositionMessage(byte[] data) {
    if (data.length < 11) {
      throw new IllegalArgumentException("Invalid MIN072 payload length");
    }

    int b0 = data[0] & 0xFF;
    int b1 = data[1] & 0xFF;
    int b2 = data[2] & 0xFF;
    int b3 = data[3] & 0xFF;
    int b4 = data[4] & 0xFF;
    int b5 = data[5] & 0xFF;
    int b6 = data[6] & 0xFF;
    int b7 = data[7] & 0xFF;
    int b8 = data[8] & 0xFF;
    int b9 = data[9] & 0xFF;
    int b10 = data[10] & 0xFF;

    fixStatus = b0;

    int rawLat = ((b1 << 16) | (b2 << 8) | b3);
    int tmplatitude = (rawLat & 0x800000) != 0 ? rawLat | 0xFF000000 : rawLat;

    int rawLong = ((b4 & 0x7F) << 18) | (b5 << 10) | (b6 << 2) | ((b7 >> 6) & 0x03);
    int tmplongitude = (rawLong & 0x1000000) != 0 ? rawLong | 0xFE000000 : rawLong;

    longitude = tmplongitude * 0.001 / 60.0;
    latitude = tmplatitude * 0.001 / 60.0;

    int rawAlt = ((b7 & 0x3F) << 9) | (b8 << 1) | ((b9 >> 7) & 0x01);
    altitude = (rawAlt & 0x4000) != 0 ? rawAlt | 0xFFFF8000 : rawAlt;

    speed = b9 & 0x7F;
    heading = b10 * 2;

    int b11 = data[11] & 0xFF;
    int b12 = data[12] & 0xFF;
    dayOfMonth = (b11 >> 3) & 0x1F;
    minuteOfDay = ((b11 & 0x07) << 8) | b12;
  }

  public LocalTime getTime() {
    return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60);
  }
}
