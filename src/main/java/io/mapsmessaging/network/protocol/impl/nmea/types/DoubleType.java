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

public class DoubleType implements Type {

  private final double value;

  public DoubleType(String value) {
    if (!value.isEmpty()) {
      this.value = Double.parseDouble(value);
    } else {
      this.value = 0.0;
    }
  }

  public double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return Double.toString(value);
  }

  @Override
  public Object jsonPack() {
    return value;
  }
}
