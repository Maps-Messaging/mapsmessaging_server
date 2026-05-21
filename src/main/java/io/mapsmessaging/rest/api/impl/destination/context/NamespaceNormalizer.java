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

package io.mapsmessaging.rest.api.impl.destination.context;

import java.util.ArrayList;
import java.util.List;

public class NamespaceNormalizer {

  private NamespaceNormalizer() {
  }

  public static String normalize(String path) {
    if (path == null) {
      return "";
    }

    String trimmed = path.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    // Keep leading "/" if present (absolute marker), but collapse repeated slashes everywhere.
    StringBuilder normalized = new StringBuilder(trimmed.length());

    boolean lastWasSlash = false;
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c == '/') {
        if (!lastWasSlash) {
          normalized.append('/');
          lastWasSlash = true;
        }
      } else {
        normalized.append(c);
        lastWasSlash = false;
      }
    }

    // Remove trailing "/" (but do not remove the single "/" root marker)
    int length = normalized.length();
    while (length > 1 && normalized.charAt(length - 1) == '/') {
      normalized.setLength(length - 1);
      length = normalized.length();
    }

    // Special-case: "/" should remain "/" (represents absolute root marker)
    if (normalized.length() == 1 && normalized.charAt(0) == '/') {
      return "/";
    }

    return normalized.toString();
  }

  public static String[] splitNormalized(String normalizedPath) {
    if (normalizedPath == null || normalizedPath.isEmpty()) {
      return new String[0];
    }

    // "/" means "absolute root marker node"
    if (normalizedPath.equals("/")) {
      return new String[]{""};
    }

    boolean absolute = normalizedPath.charAt(0) == '/';

    int startIndex = absolute ? 1 : 0;
    List<String> segments = new ArrayList<>();

    // Absolute marker segment must be preserved as the first segment
    if (absolute) {
      segments.add("");
    }

    int i = startIndex;
    int segmentStart = startIndex;

    while (i <= normalizedPath.length()) {
      boolean atEnd = i == normalizedPath.length();
      if (atEnd || normalizedPath.charAt(i) == '/') {
        if (i > segmentStart) {
          segments.add(normalizedPath.substring(segmentStart, i));
        }
        segmentStart = i + 1;
      }
      i++;
    }

    return segments.toArray(new String[0]);
  }
}
