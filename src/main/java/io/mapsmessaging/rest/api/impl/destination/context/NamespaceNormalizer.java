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

  /**
   * Normalizes per your server rules:
   * - Preserves leading '/' distinction
   * - Collapses multiple '/' anywhere to a single '/'
   * - Removes trailing '/' (except when the whole path is "/")
   */
  public static String normalize(String path) {
    if (path == null) {
      return "";
    }

    String trimmed = path.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    boolean hasLeadingSlash = trimmed.charAt(0) == '/';

    StringBuilder stringBuilder = new StringBuilder(trimmed.length());
    boolean lastWasSlash = false;

    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c == '/') {
        if (!lastWasSlash) {
          stringBuilder.append('/');
          lastWasSlash = true;
        }
        continue;
      }
      stringBuilder.append(c);
      lastWasSlash = false;
    }

    String collapsed = stringBuilder.toString();

    if (!hasLeadingSlash && collapsed.startsWith("/")) {
      collapsed = collapsed.substring(1);
    }

    if (collapsed.length() > 1 && collapsed.endsWith("/")) {
      collapsed = collapsed.substring(0, collapsed.length() - 1);
    }

    if (collapsed.equals("/")) {
      return "/";
    }

    if (collapsed.isEmpty()) {
      return "";
    }

    return collapsed;
  }

  /**
   * Split a *normalized* path into segments.
   * For absolute paths (leading '/'), first segment is "" to preserve MQTT distinction.
   */
  public static String[] splitNormalized(String normalizedPath) {
    if (normalizedPath.equals("/")) {
      return new String[]{""};
    }

    boolean absolute = normalizedPath.startsWith("/");
    String working = absolute ? normalizedPath.substring(1) : normalizedPath;

    if (working.isEmpty()) {
      return absolute ? new String[]{""} : new String[0];
    }

    List<String> segments = new ArrayList<>();
    if (absolute) {
      segments.add("");
    }

    int startIndex = 0;
    for (int i = 0; i < working.length(); i++) {
      if (working.charAt(i) == '/') {
        segments.add(working.substring(startIndex, i));
        startIndex = i + 1;
      }
    }
    segments.add(working.substring(startIndex));

    return segments.toArray(new String[0]);
  }
}
