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

package io.mapsmessaging.network.io.security;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

public class SharedKeyHelper {

  public static byte[] convertKey(String key) {
    key = key.trim();
    if (key.startsWith("0x")) {
      return fromHex(key);
    }
    return key.getBytes();
  }

  private static byte[] fromHex(String hexStr) {
    StringTokenizer st = new StringTokenizer(hexStr, ",");
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    while (st.hasMoreElements()) {
      String val = (String) st.nextElement();
      val = val.trim();
      int v = 0;
      if (val.startsWith("0x")) {
        v = Integer.parseInt(val.substring(2), 16);
      } else {
        v = Integer.parseInt(val, 10);
      }
      baos.write(v);
    }
    return baos.toByteArray();
  }

  private SharedKeyHelper() {
  }
}
