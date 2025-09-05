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


import java.time.LocalDate;

public class DateType implements Type {

  private final LocalDate localDate;

  public DateType(String date) {
    int numeric = Integer.parseInt(date);
    int day = (numeric / 10000) % 100;
    int month = (numeric / 100) % 100;
    int year = numeric % 100 + 2000;

    localDate = LocalDate.of(year, month, day);
  }

  public LocalDate getDate() {
    return localDate;
  }

  @Override
  public String toString() {
    return localDate.toString();
  }

  @Override
  public Object jsonPack() {
    return localDate.toString();
  }

}