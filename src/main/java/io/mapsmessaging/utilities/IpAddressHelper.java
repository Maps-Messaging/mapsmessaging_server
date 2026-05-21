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

package io.mapsmessaging.utilities;

public class IpAddressHelper {
  public static String normalizeIp(String raw) {
    if (raw == null || raw.isBlank()) {
      return "unknown";
    }

    String ip = raw.trim();

    // Strip leading slash
    if (ip.startsWith("/")) {
      ip = ip.substring(1);
    }

    // IPv6 with port: [::1]:1234
    if (ip.startsWith("[") && ip.contains("]")) {
      return ip.substring(1, ip.indexOf(']'));
    }

    // IPv4 with port: 1.2.3.4:5678
    int colon = ip.lastIndexOf(':');
    if (colon > 0 && ip.indexOf('.') != -1) {
      ip = ip.substring(0, colon);
    }

    return ip;
  }
}
