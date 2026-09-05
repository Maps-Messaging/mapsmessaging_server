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
 */

package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

final class SchemaMatchSelector {

  private static final int EXACT_NAME_SCORE = 400;
  private static final int EXACT_UNIQUE_ID_SCORE = 350;
  private static final int PRELOADED_ALIAS_SCORE = 300;
  private static final int QUALIFIED_SUFFIX_SCORE = 200;
  private static final int LOOSE_SUFFIX_SCORE = 100;

  private SchemaMatchSelector() {}

  static SchemaConfig select(
      String name,
      String type,
      SchemaConfig preloaded,
      Collection<SchemaConfig> candidates) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(candidates, "candidates cannot be null");

    Match best = null;

    for (SchemaConfig candidate : candidates) {
      Match match = score(candidate, candidate == preloaded, name, type);
      if (match != null && (best == null || MATCH_ORDER.compare(match, best) < 0)) {
        best = match;
      }
    }

    if (preloaded != null && !candidates.contains(preloaded)) {
      Match match = score(preloaded, true, name, type);
      if (match != null && (best == null || MATCH_ORDER.compare(match, best) < 0)) {
        best = match;
      }
    }

    return best == null ? null : best.schemaConfig();
  }

  private static Match score(
      SchemaConfig schemaConfig, boolean preloadedAlias, String name, String type) {
    if (schemaConfig == null
        || schemaConfig.getFormat() == null
        || !schemaConfig.getFormat().equalsIgnoreCase(type)) {
      return null;
    }

    String schemaName = schemaConfig.getName();
    String uniqueId = schemaConfig.getUniqueId();
    int score = -1;

    if (schemaName != null && schemaName.equalsIgnoreCase(name)) {
      score = EXACT_NAME_SCORE;
    } else if (uniqueId != null && uniqueId.equalsIgnoreCase(name)) {
      score = EXACT_UNIQUE_ID_SCORE;
    } else if (preloadedAlias) {
      score = PRELOADED_ALIAS_SCORE;
    } else if (qualifiedSuffixMatch(schemaName, name)) {
      score = QUALIFIED_SUFFIX_SCORE;
    } else if (schemaName != null && endsWithIgnoreCase(schemaName, name)) {
      score = LOOSE_SUFFIX_SCORE;
    }

    return score < 0 ? null : new Match(schemaConfig, score, schemaName == null ? Integer.MAX_VALUE : schemaName.length());
  }

  private static boolean qualifiedSuffixMatch(String schemaName, String name) {
    if (schemaName == null || schemaName.length() <= name.length()) {
      return false;
    }

    int start = schemaName.length() - name.length();
    return schemaName.regionMatches(true, start, name, 0, name.length())
        && schemaName.charAt(start - 1) == '.';
  }

  private static boolean endsWithIgnoreCase(String value, String suffix) {
    if (value.length() < suffix.length()) {
      return false;
    }
    return value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
  }

  private static final Comparator<Match> MATCH_ORDER =
      Comparator.comparingInt(Match::score)
          .reversed()
          .thenComparingInt(Match::nameLength)
          .thenComparing(
              match -> Objects.toString(match.schemaConfig().getName(), ""),
              String.CASE_INSENSITIVE_ORDER)
          .thenComparing(
              match -> Objects.toString(match.schemaConfig().getUniqueId(), ""),
              String.CASE_INSENSITIVE_ORDER);

  private record Match(SchemaConfig schemaConfig, int score, int nameLength) {}
}
