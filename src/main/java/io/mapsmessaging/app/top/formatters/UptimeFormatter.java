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

package io.mapsmessaging.app.top.formatters;

public class UptimeFormatter implements Formatter {

  private final int len;
  private final boolean prepend;

  public UptimeFormatter(int len, boolean prepend) {
    this.len = len;
    this.prepend = prepend;
  }

  public String format(Object value) {
    if (value instanceof Long) {
      return pad(formatUptime((Long) value), len, prepend);
    }
    return "";
  }

  private String formatUptime(long uptimeMillis) {
    long days = uptimeMillis / (24L * 60L * 60L * 1000L);
    long uptimeSeconds = uptimeMillis / 1000;
    long hours = (uptimeSeconds / 3600) % 24;
    long minutes = (uptimeSeconds % 3600) / 60;
    long seconds = uptimeSeconds % 60;
    String val = String.format("%02d:%02d:%02d", hours, minutes, seconds);
    if (days != 0) {
      val = val + " " + days + " days";
    }
    return val;
  }
}
