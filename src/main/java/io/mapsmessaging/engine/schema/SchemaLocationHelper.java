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

package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaResource;

import java.time.OffsetDateTime;
import java.util.*;

public class SchemaLocationHelper {

  private SchemaLocationHelper() {}

  public static int compareVersionStrings(String v1, String v2) {
    if (v1 == null && v2 == null) return 0;
    if (v1 == null) return -1;
    if (v2 == null) return 1;

    String s1 = v1.trim().toLowerCase(Locale.ROOT).replaceFirst("^v", "");
    String s2 = v2.trim().toLowerCase(Locale.ROOT).replaceFirst("^v", "");

    // strip build metadata (+build)
    s1 = s1.split("\\+")[0];
    s2 = s2.split("\\+")[0];

    // try semver-ish numeric split
    String[] p1 = s1.split("[\\.-]");
    String[] p2 = s2.split("[\\.-]");
    int len = Math.max(p1.length, p2.length);

    for (int i = 0; i < len; i++) {
      String a = i < p1.length ? p1[i] : "0";
      String b = i < p2.length ? p2[i] : "0";

      boolean aNum = a.matches("\\d+");
      boolean bNum = b.matches("\\d+");

      if (aNum && bNum) {
        int diff = Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        if (diff != 0) return diff;
      } else if (aNum != bNum) {
        return aNum ? 1 : -1; // numeric beats non-numeric
      } else {
        int diff = a.compareTo(b);
        if (diff != 0) return diff;
      }
    }

    // tie-breaker for prerelease (alpha/beta/rc etc)
    List<String> order = List.of("snapshot", "dev", "alpha", "beta", "rc", "");
    int idx1 = order.indexOf(order.stream().filter(s1::contains).findFirst().orElse(""));
    int idx2 = order.indexOf(order.stream().filter(s2::contains).findFirst().orElse(""));
    return Integer.compare(idx1, idx2);
  }



  public static SchemaConfig locateSchema(List<SchemaResource> resources, String destinationName) {
    OffsetDateTime now = OffsetDateTime.now();
    Map<String, SchemaConfig> bestVersionPerId = new HashMap<>();

    for (SchemaResource resource : resources) {
      for (SchemaConfig schema : resource.getAll()) {
        if (!isActive(schema, now)) continue;

        boolean matches = destinationName.equals(schema.getTitle());
        if (!matches) {
          String expr = schema.getMatchExpression();
          if (expr != null && !expr.isEmpty()) {
            matches = destinationName.matches(expr);
          }
        }

        if (matches) {
          String id = schema.getUniqueId();
          SchemaConfig existing = bestVersionPerId.get(id);
          if (existing == null || compareVersionStrings(schema.getVersion(), existing.getVersion()) > 0) {
            bestVersionPerId.put(id, schema);
          }
        }
      }
    }

    // Now select the best scored match
    return bestVersionPerId.values().stream()
        .max(Comparator.comparingInt(s -> scoreMatch(s, destinationName)))
        .orElse(null);
  }

  private static boolean isActive(SchemaConfig schema, OffsetDateTime now) {
    OffsetDateTime start = schema.getNotBefore();
    OffsetDateTime end = schema.getExpiresAfter();
    return (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end));
  }

  // Example scoring: higher if exact match, otherwise use regex specificity
  private static int scoreMatch(SchemaConfig schema, String target) {
    if (target.equals(schema.getName())) return Integer.MAX_VALUE;
    String expr = schema.getMatchExpression();
    if (expr == null) return 0;
    return expr.length(); // crude proxy: longer regex = more specific
  }

}