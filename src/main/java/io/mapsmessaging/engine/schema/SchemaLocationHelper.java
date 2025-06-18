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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaLocationHelper {

  public static SchemaConfig locateSchema(List<SchemaConfig> schemas, String destinationName) {
    LocalDateTime now = LocalDateTime.now();
    Map<String, SchemaConfig> bestVersionPerId = new HashMap<>();

    for (SchemaConfig schema : schemas) {
      if (!isActive(schema, now)) continue;
      if (!isActive(schema, now)) continue;

      boolean matches = destinationName.equals(schema.getTitle());
      if (!matches) {
        String expr = schema.getMatchExpression();
        if(expr != null && !expr.isEmpty()) {
          matches = destinationName.matches(expr);
        }
      }

      if (matches) {
        String id = schema.getUniqueId();
        SchemaConfig existing = bestVersionPerId.get(id);
        if (existing == null || schema.getVersion() > existing.getVersion()) {
          bestVersionPerId.put(id, schema);
        }
      }
    }

    // Now select the best scored match
    return bestVersionPerId.values().stream()
        .max(Comparator.comparingInt(s -> scoreMatch(s, destinationName)))
        .orElse(null);
  }

  private static boolean isActive(SchemaConfig schema, LocalDateTime now) {
    LocalDateTime start = schema.getNotBefore();
    LocalDateTime end = schema.getExpiresAfter();
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
