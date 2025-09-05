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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import java.util.Set;
import java.util.regex.Pattern;

public final class DeviceIdUtil {
  private static final Set<String> ALLOWED_MFR = Set.of("SKY", "QCS", "UBX");
  private static final Pattern HEX = Pattern.compile("^[0-9a-fA-F]+$");

  public static boolean isValidDeviceId(String str) {
    if (str == null) return false;
    int len = str.length();
    if (len != 15 && len != 16) return false;

    int mtidLen = (len == 15) ? 8 : 9;
    String mtid = str.substring(0, mtidLen);
    for (int i = 0; i < mtid.length(); i++) {
      if (!Character.isDigit(mtid.charAt(i))) return false;
    }

    String mfr = str.substring(mtidLen, mtidLen + 3);
    if (!ALLOWED_MFR.contains(mfr)) return false;

    String checksum = str.substring(mtidLen + 3);
    return HEX.matcher(checksum).matches();
  }

  private DeviceIdUtil() {
    // static only
  }
}
