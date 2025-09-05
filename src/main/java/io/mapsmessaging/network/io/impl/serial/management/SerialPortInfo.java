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
package io.mapsmessaging.network.io.impl.serial.management;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class SerialPortInfo implements Comparable<SerialPortInfo> {
  private final String name;
  private final String serialNumber;

  @Override
  public int compareTo(SerialPortInfo other) {
    int serialCompare = this.serialNumber.compareToIgnoreCase(other.serialNumber);
    return serialCompare != 0 ? serialCompare : this.name.compareToIgnoreCase(other.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SerialPortInfo that = (SerialPortInfo) o;
    return name.equalsIgnoreCase(that.name) && serialNumber.equalsIgnoreCase(that.serialNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name.toLowerCase(), serialNumber.toLowerCase());
  }

}
