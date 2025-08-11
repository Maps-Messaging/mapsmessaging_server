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

public class HeightType implements Type {

  private final double height;
  private final char unit;

  public HeightType(String value, String unit) {
    double t = 0.0d;
    try {
      t = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      //Ignore, it could simply be blank
    }
    height = t;
    if(!unit.isEmpty()) {
      this.unit = unit.toUpperCase(Locale.ROOT).charAt(0);
    }
    else{
      this.unit = 'M';
    }
  }

  public char getUnit() {
    return unit;
  }

  public double getHeight() {
    return height;
  }

  @Override
  public String toString() {
    return "" + height + "," + unit;
  }

  @Override
  public Object jsonPack() {
    return height + " " + unit;
  }

}