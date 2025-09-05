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

package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.Locale;

public class PositionType implements Type {

  private final int degrees;
  private final double minutes;
  private final char direction;

  public PositionType(String value, String dir) {
    double min = 0;
    int deg = 0;
    char dire = ' ';
    if (!value.isEmpty()) {
      double numeric = Double.parseDouble(value);
      deg = (int) (numeric / 100);
      min = (numeric % 100.0f) / 60.0f;
    }
    if (!dir.isEmpty()) {
      dire = dir.toUpperCase(Locale.ROOT).charAt(0);
    }
    minutes = min;
    this.degrees = deg;
    direction = dire;
  }

  public char getDirection() {
    return direction;
  }

  public double getPosition() {
    int indicator = 1;
    if (direction == 'S' || direction == 'W') {
      indicator = -1;
    }
    return (degrees + minutes) * indicator;
  }


  @Override
  public Object jsonPack() {
    return this;
  }

  @Override
  public String toString() {
    return "" + ((degrees * 100) + (minutes * 60)) + "," + direction;
  }
}
