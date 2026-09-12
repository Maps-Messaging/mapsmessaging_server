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
package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.config.CotConfigDTO;
import java.util.ArrayList;
import java.util.List;

public final class CotConfigResolver {

  private final List<CotConfigDTO> configs;

  public CotConfigResolver(List<CotConfigDTO> configs) {
    this.configs = configs == null ? List.of() : List.copyOf(configs);
  }

  public CotConfigDTO resolve(String namespace) {
    if (namespace == null || namespace.isBlank()) {
      return null;
    }

    CotConfigDTO best = null;
    int bestScore = Integer.MIN_VALUE;
    for (CotConfigDTO config : configs) {
      if (config == null || !matches(config.getNamespacePath(), namespace)) {
        continue;
      }
      int score = specificity(config.getNamespacePath());
      if (score > bestScore) {
        best = config;
        bestScore = score;
      }
    }
    return best;
  }

  static boolean matches(String pattern, String namespace) {
    if (pattern == null || pattern.isBlank() || namespace == null || namespace.isBlank()) {
      return false;
    }

    List<String> patternLevels = levels(pattern);
    List<String> namespaceLevels = levels(namespace);
    int namespaceIndex = 0;

    for (int patternIndex = 0; patternIndex < patternLevels.size(); patternIndex++) {
      String patternLevel = patternLevels.get(patternIndex);
      if ("#".equals(patternLevel)) {
        return patternIndex == patternLevels.size() - 1;
      }
      if (namespaceIndex >= namespaceLevels.size()) {
        return false;
      }
      if (!"+".equals(patternLevel) && !patternLevel.equals(namespaceLevels.get(namespaceIndex))) {
        return false;
      }
      namespaceIndex++;
    }
    return namespaceIndex == namespaceLevels.size();
  }

  private static int specificity(String pattern) {
    int score = 0;
    for (String level : levels(pattern)) {
      if ("#".equals(level)) {
        score += 1;
      } else if ("+".equals(level)) {
        score += 10;
      } else {
        score += 100;
      }
    }
    return score;
  }

  private static List<String> levels(String path) {
    String[] rawLevels = path.trim().split("/", -1);
    List<String> result = new ArrayList<>(rawLevels.length);
    for (String level : rawLevels) {
      if (!level.isEmpty()) {
        result.add(level);
      }
    }
    return result;
  }
}
