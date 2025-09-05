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

package io.mapsmessaging.network.io.impl.tcp;

import java.net.InetAddress;

public class NetworkHelper {

  public static boolean isInCidr(String cidr, String ip) {
    try {
      String[] parts = cidr.split("/");
      InetAddress cidrAddress = InetAddress.getByName(parts[0]);
      InetAddress targetAddress = InetAddress.getByName(ip);
      int prefixLength = Integer.parseInt(parts[1]);

      byte[] cidrBytes = cidrAddress.getAddress();
      byte[] ipBytes = targetAddress.getAddress();

      if (cidrBytes.length != ipBytes.length) return false; // IPv4 vs IPv6 mismatch

      int byteCount = prefixLength / 8;
      int bitRemainder = prefixLength % 8;

      for (int i = 0; i < byteCount; i++) {
        if (cidrBytes[i] != ipBytes[i]) return false;
      }

      if (bitRemainder > 0) {
        int mask = ~((1 << (8 - bitRemainder)) - 1);
        if ((cidrBytes[byteCount] & mask) != (ipBytes[byteCount] & mask)) return false;
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
