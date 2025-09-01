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

package io.mapsmessaging.network.protocol.impl.satellite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedAt {
  private String original;   // Full line as received
  private String command;    // e.g., "ATI", "AT+CSQ", "AT+FOO=1,2", "AT+BAR?", "AT+BAZ=?"
  private String name;       // "", "I", "+CSQ", "+FOO", "+BAR", "+BAZ"
  private String params;     // after '=', may be null
  private boolean query;     // "...?"
  private boolean test;      // "...=?"

  public static ParsedAt parse(String line) {
    String trimmed = line.trim();
    String rem = trimmed.substring(2); // after "AT"
    rem = rem.trim();                // may be empty (plain "AT")
    String name;
    String params = null;
    boolean isTest = false;
    boolean isQuery = false;

    if (rem.isEmpty()) {
      name = ""; // plain "AT"
    } else {
      // Detect test ("=?") and query ("?") forms
      if (rem.endsWith("=?")) { isTest = true; }
      if (!isTest && rem.endsWith("?")) { isQuery = true; }

      int eq = rem.indexOf('=');
      if (eq >= 0) {
        name = rem.substring(0, eq);
        params = rem.substring(eq + 1);
      } else {
        name = rem;
      }

      // Normalize name for things like "+CSQ?" → "+CSQ"
      if (name.endsWith("?")) {
        name = name.substring(0, name.length() - 1);
        isQuery = true;
      }
    }

    return new ParsedAt(trimmed, rem, name, params, isQuery, isTest);
  }
}