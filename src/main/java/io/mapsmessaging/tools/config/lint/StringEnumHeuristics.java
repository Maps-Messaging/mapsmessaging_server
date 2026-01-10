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

package io.mapsmessaging.tools.config.lint;


import java.util.Set;

public final class StringEnumHeuristics {

  private static final Set<String> OPEN_VOCAB_FIELDS = Set.of(
      "contenttype",
      "mediatype",
      "mimetype"
  );

  private static final Set<String> TOKENS = Set.of(
      "type",
      "mode",
      "level",
      "status",
      "state",
      "policy",
      "protocol",
      "format",
      "kind",
      "strategy",
      "role"
  );

  private StringEnumHeuristics() {
  }

  public static boolean looksLikeEnum(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }

    String lower = fieldName.toLowerCase();

    if (OPEN_VOCAB_FIELDS.contains(lower)) {
      return false;
    }

    for (String token : TOKENS) {
      if (lower.equals(token)) {
        return true;
      }
      if (lower.endsWith(token)) {
        return true;
      }
      if (lower.contains(token)) {
        return true;
      }
    }

    return false;
  }

}
