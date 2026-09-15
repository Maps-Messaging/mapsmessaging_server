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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Converts modern NATO/MIL-STD-2525 entity classification into a Cursor-on-Target atom type. */
final class NatoCotTypeMapper {

  private static final String LEGACY_LOOKUP_RESOURCE = "io/mapsmessaging/state/drone/tak/2525-legacy-atomic.json";
  private static final Pattern NUMERIC_SIDC = Pattern.compile("1[0-3]\\d{18}");
  private static final Pattern TWO_DIGITS = Pattern.compile("\\d{2}");
  private static final Pattern TEN_DIGITS = Pattern.compile("\\d{10}");

  private static final Map<String, String> AFFILIATION_BY_SIDC =
      Map.ofEntries(
          Map.entry("00", "p"),
          Map.entry("01", "u"),
          Map.entry("02", "a"),
          Map.entry("03", "f"),
          Map.entry("04", "n"),
          Map.entry("05", "s"),
          Map.entry("06", "h"),
          Map.entry("10", "p"),
          Map.entry("11", "u"),
          Map.entry("12", "a"),
          Map.entry("13", "f"),
          Map.entry("14", "n"),
          Map.entry("15", "j"),
          Map.entry("16", "k"));

  private static final Map<String, String> IDENTITY_TO_SIDC =
      Map.ofEntries(
          Map.entry("PENDING", "00"),
          Map.entry("UNKNOWN", "01"),
          Map.entry("ASSUMED_FRIEND", "02"),
          Map.entry("FRIEND", "03"),
          Map.entry("NEUTRAL", "04"),
          Map.entry("SUSPECT", "05"),
          Map.entry("HOSTILE", "06"),
          Map.entry("JOKER", "15"),
          Map.entry("FAKER", "16"));

  private static final Map<String, String> SYMBOL_SET_TO_CODE =
      Map.ofEntries(
          Map.entry("AIR", "01"),
          Map.entry("AIR_MISSILE", "02"),
          Map.entry("SPACE", "05"),
          Map.entry("SPACE_MISSILE", "06"),
          Map.entry("LAND_UNIT", "10"),
          Map.entry("LAND_CIVILIAN_UNIT", "11"),
          Map.entry("LAND_CIVILIAN_ORGANIZATION", "11"),
          Map.entry("LAND_EQUIPMENT", "15"),
          Map.entry("LAND_INSTALLATIONS", "20"),
          Map.entry("DISMOUNTED_INDIVIDUAL", "27"),
          Map.entry("SEA_SURFACE", "30"),
          Map.entry("SEA_SUBSURFACE", "35"),
          Map.entry("MINE_WARFARE", "36"),
          Map.entry("SIGNALS_INTELLIGENCE_SPACE", "50"),
          Map.entry("SIGNALS_INTELLIGENCE_AIR", "51"),
          Map.entry("SIGNALS_INTELLIGENCE_LAND", "52"),
          Map.entry("SIGNALS_INTELLIGENCE_SURFACE", "53"),
          Map.entry("SIGNALS_INTELLIGENCE_SUBSURFACE", "54"));

  private static final Map<String, String> DIMENSION_BY_SYMBOL_SET =
      Map.ofEntries(
          Map.entry("01", "A"),
          Map.entry("02", "A"),
          Map.entry("05", "P"),
          Map.entry("06", "P"),
          Map.entry("10", "G"),
          Map.entry("11", "G"),
          Map.entry("15", "G"),
          Map.entry("20", "G"),
          Map.entry("27", "G"),
          Map.entry("30", "S"),
          Map.entry("35", "U"),
          Map.entry("36", "U"),
          Map.entry("50", "P"),
          Map.entry("51", "A"),
          Map.entry("52", "G"),
          Map.entry("53", "S"),
          Map.entry("54", "U"));

  private final Map<String, String> legacyLookup;

  NatoCotTypeMapper() {
    legacyLookup = LegacyLookupHolder.LOOKUP;
  }

  String fromDescription(Map<String, Object> description) {
    if (description == null || description.isEmpty()) {
      return null;
    }

    String directSidc = stringValue(first(description, "sidc", "symbol_id", "symbolId", "symbol_identification_code", "symbolIdentificationCode"));
    if (directSidc != null && NUMERIC_SIDC.matcher(directSidc).matches()) {
      return fromNumericSidc(directSidc);
    }

    String identity = standardIdentityCode(first(description, "standard_identity", "standardIdentity"));
    String symbolSet = symbolSetCode(first(description, "symbol_set", "symbolSet"));
    if (identity == null || symbolSet == null || !DIMENSION_BY_SYMBOL_SET.containsKey(symbolSet)) {
      return null;
    }

    String entity = componentCode(first(description, "entity"), "00");
    String entityType = componentCode(first(description, "entity_type", "entityType"), "00");
    String entitySubtype = componentCode(first(description, "entity_subtype", "entitySubtype"), "00");
    String sector1 = componentCode(first(description, "sector_1", "sector1"), "00");
    String sector2 = componentCode(first(description, "sector_2", "sector2"), "00");

    String sidc = "10" + identity + symbolSet + "0" + "000" + entity + entityType + entitySubtype + sector1 + sector2;
    return fromNumericSidc(sidc);
  }

  String fromNumericSidc(String sidc) {
    if (sidc == null || !NUMERIC_SIDC.matcher(sidc).matches()) {
      return null;
    }

    String affiliation = AFFILIATION_BY_SIDC.get(sidc.substring(2, 4));
    String symbolSet = sidc.substring(4, 6);
    String dimension = DIMENSION_BY_SYMBOL_SET.get(symbolSet);
    if (affiliation == null || dimension == null) {
      return null;
    }

    String basic = "a-" + affiliation + "-" + dimension;
    String legacy = findLegacyCode(sidc, symbolSet);
    if (legacy == null || legacy.length() < 10 || legacy.charAt(0) != 'S' || !dimension.equals(String.valueOf(legacy.charAt(2)))) {
      return basic;
    }

    String functionId = legacy.substring(4, 10).replaceAll("[^A-Z0-9]", "");
    if (functionId.isEmpty()) {
      return basic;
    }

    StringBuilder cotType = new StringBuilder(basic);
    for (int i = 0; i < functionId.length(); i++) {
      cotType.append('-').append(functionId.charAt(i));
    }
    return cotType.toString();
  }

  private String findLegacyCode(String sidc, String symbolSet) {
    String entity = sidc.substring(10, 12);
    String entityType = sidc.substring(12, 14);
    String entitySubtype = sidc.substring(14, 16);
    String modifier1 = sidc.substring(16, 18);
    String modifier2 = sidc.substring(18, 20);

    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(entity + entityType + entitySubtype + modifier1 + modifier2);
    candidates.add(entity + entityType + entitySubtype + modifier1 + "00");
    candidates.add(entity + entityType + entitySubtype + "0000");
    candidates.add(entity + entityType + "000000");
    candidates.add(entity + "00000000");

    for (String candidate : candidates) {
      String legacy = legacyLookup.get(symbolSet + ':' + candidate);
      if (legacy != null) {
        return legacy;
      }
    }
    return null;
  }

  private String standardIdentityCode(Object value) {
    String text = stringValue(value);
    if (text == null) {
      return null;
    }
    if (TWO_DIGITS.matcher(text).matches() && AFFILIATION_BY_SIDC.containsKey(text)) {
      return text;
    }
    return IDENTITY_TO_SIDC.get(stripPrefix(text, "StandardIdentityEnum_"));
  }

  private String symbolSetCode(Object value) {
    String text = stringValue(value);
    if (text == null) {
      return null;
    }
    if (TWO_DIGITS.matcher(text).matches() && DIMENSION_BY_SYMBOL_SET.containsKey(text)) {
      return text;
    }
    return SYMBOL_SET_TO_CODE.get(stripPrefix(text, "SymbolSetEnum_"));
  }

  private String componentCode(Object value, String defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      int numericValue = number.intValue();
      return numericValue >= 0 && numericValue <= 99 ? String.format(Locale.ROOT, "%02d", numericValue) : defaultValue;
    }

    String text = stringValue(value);
    if (text == null) {
      return defaultValue;
    }
    if (text.matches("\\d")) {
      return '0' + text;
    }
    if (TWO_DIGITS.matcher(text).matches()) {
      return text;
    }

    int underscore = text.lastIndexOf('_');
    if (underscore >= 0 && underscore + 1 < text.length()) {
      String suffix = text.substring(underscore + 1);
      if (suffix.matches("\\d")) {
        return '0' + suffix;
      }
      if (TWO_DIGITS.matcher(suffix).matches()) {
        return suffix;
      }
    }
    return defaultValue;
  }

  private Object first(Map<String, Object> description, String... keys) {
    for (String key : keys) {
      if (description.containsKey(key)) {
        return description.get(key);
      }
    }
    return null;
  }

  private String stripPrefix(String value, String prefix) {
    String upperValue = value.toUpperCase(Locale.ROOT);
    String upperPrefix = prefix.toUpperCase(Locale.ROOT);
    return upperValue.startsWith(upperPrefix) ? upperValue.substring(upperPrefix.length()) : upperValue;
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isEmpty() ? null : text;
  }

  private static Map<String, String> loadLegacyLookup() {
    InputStream input = NatoCotTypeMapper.class.getClassLoader().getResourceAsStream(LEGACY_LOOKUP_RESOURCE);
    if (input == null) {
      throw new IllegalStateException("Missing MIL-STD-2525 lookup resource " + LEGACY_LOOKUP_RESOURCE);
    }

    Map<String, String> lookup = new LinkedHashMap<>();
    try (input) {
      JsonNode root = new ObjectMapper().readTree(input);
      if (!root.isArray()) {
        throw new IllegalStateException("MIL-STD-2525 lookup resource is not an array");
      }
      for (JsonNode row : root) {
        if (!row.isArray() || row.size() < 3) {
          continue;
        }
        String legacy = row.get(0).asText();
        String symbolSet = row.get(1).asText();
        String numericCode = row.get(2).asText();
        if (!legacy.startsWith("S") || !TWO_DIGITS.matcher(symbolSet).matches() || !TEN_DIGITS.matcher(numericCode).matches()) {
          continue;
        }
        lookup.putIfAbsent(symbolSet + ':' + numericCode, legacy);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to load MIL-STD-2525 lookup data", exception);
    }
    return Map.copyOf(lookup);
  }

  private static final class LegacyLookupHolder {
    private static final Map<String, String> LOOKUP = loadLegacyLookup();
  }
}
