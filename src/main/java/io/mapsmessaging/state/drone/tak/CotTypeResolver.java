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

import io.mapsmessaging.cot.types.Affiliation;
import io.mapsmessaging.cot.types.BattleDimension;
import io.mapsmessaging.cot.types.CotType;
import io.mapsmessaging.cot.types.CotTypeRegistry;
import io.mapsmessaging.cot.types.FunctionKey;
import io.mapsmessaging.state.config.VehicleClass;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Chooses the CoT atom type of a twin from the CoT type table ({@code io.mapsmessaging:cot}).
 *
 * <p>The battle dimension and function come from, in order: the twin's vehicle class, when it
 * has one (MAVLink vehicles); the MIL-STD-2525D symbol of its STANAG 4817 description -- the
 * {@code symbol_set} and the {@code entity}, {@code entity_type} and {@code entity_subtype} codes
 * -- translated through the table's 2525D crosswalk, which falls back to the parent entity when a
 * symbol has no 2525C counterpart; the twin's STANAG 4817 node specialization, when it names a
 * platform the table can place and does not contradict the symbol set; the dimension of the
 * {@code symbol_set} alone; otherwise the "other" dimension. A vehicle-class type is checked against the table and falls back to its
 * dimension alone when the table does not carry it.
 */
final class CotTypeResolver {

  private static final String SYMBOL_SET_PREFIX = "SYMBOLSETENUM_";
  private static final String SPECIALIZATION_PREFIX = "NODESPECIALIZATIONTYPEENUM_";
  private static final String DISCRIMINATOR = "$discriminator";

  /**
   * The CoT function of a STANAG 4817 node specialization, where the type table carries one.
   * A single letter is a battle dimension and nothing more: a crewed vessel says which medium it
   * moves in, not that it is a combatant. Values the table cannot place (OTHER, VEHICLE, and the
   * sensors with no physical counterpart) are deliberately absent -- they say nothing the symbol
   * set has not said already.
   */
  private static final Map<String, String> SPECIALIZATION_TYPES = Map.ofEntries(
      Map.entry("SURFACE_UNMANNED_SYSTEM", "S-C-U"),      // unmanned surface water vehicle
      Map.entry("SUBSURFACE_UNMANNED_SYSTEM", "U-S-U"),   // autonomous / unmanned underwater vehicle
      Map.entry("GROUND_UNMANNED_SYSTEM", "G-U-C-V-U"),   // unmanned systems
      Map.entry("SURFACE_VESSEL", "S"),
      Map.entry("VESSEL", "S"),
      Map.entry("SUBSURFACE_VESSEL", "U"),
      Map.entry("AIRCRAFT", "A"),
      Map.entry("SPACECRAFT", "P"),
      Map.entry("LAND_VEHICLE", "G"),
      Map.entry("RADAR", "G-E-S-R"),
      Map.entry("CBRN_SENSOR", "G-E-X-N"),
      // the table has one sensor symbol: every other sensing specialization resolves to it
      Map.entry("SENSOR", "G-E-S"),
      Map.entry("ACOUSTIC_SENSOR", "G-E-S"),
      Map.entry("SONAR", "G-E-S"),
      Map.entry("INFRARED_SENSOR", "G-E-S"),
      Map.entry("MAGNETIC_FIELD_SENSOR", "G-E-S"),
      Map.entry("OPTICAL_SENSOR", "G-E-S"),
      Map.entry("TACTILE_SENSOR", "G-E-S"),
      Map.entry("SOUND_RANGING_SENSOR", "G-E-S"),
      Map.entry("VERTICAL_LINE_ARRAY_DIFAR", "G-E-S"),
      Map.entry("ELECTROMAGNETIC_SPECTRUM_SENSOR", "G-E-S"),
      Map.entry("OTHER_ELECTROMAGNETIC_SPECTRUM_SENSOR", "G-E-S"));

  private final CotTypeRegistry registry;

  CotTypeResolver() {
    this(Registry.INSTANCE);
  }

  CotTypeResolver(CotTypeRegistry registry) {
    this.registry = registry;
  }

  String resolve(Affiliation affiliation, VehicleClass vehicleClass, Map<String, Object> description) {
    return resolve(affiliation, vehicleClass, description, null);
  }

  String resolve(Affiliation affiliation, VehicleClass vehicleClass, Map<String, Object> description,
      Map<String, Object> specialization) {
    Affiliation resolvedAffiliation = affiliation == null ? Affiliation.UNKNOWN : affiliation;
    if (vehicleClass != null && vehicleClass != VehicleClass.UNKNOWN) {
      return vehicleType(resolvedAffiliation, vehicleClass).toString();
    }
    Optional<CotType> symbol = entitySymbol(resolvedAffiliation, description);
    if (symbol.isPresent()) {
      return symbol.get().toString();
    }
    BattleDimension dimension = symbolSetDimension(description);
    return specializationType(resolvedAffiliation, specialization, dimension)
        .orElseGet(() -> CotType.of(resolvedAffiliation, dimension, FunctionKey.empty()))
        .toString();
  }

  /**
   * The type of the node's STANAG 4817 specialization, when it names a platform the table can
   * place. It is ignored when it contradicts the symbol set: the set is the 2525 statement, a
   * disagreement is an error at the source, and moving a sea track into the air dimension would
   * be worse than saying less about it.
   */
  private Optional<CotType> specializationType(Affiliation affiliation, Map<String, Object> specialization,
      BattleDimension symbolSetDimension) {
    String name = specializationName(specialization);
    String typeKey = name == null ? null : SPECIALIZATION_TYPES.get(name);
    if (typeKey == null) {
      return Optional.empty();
    }
    BattleDimension dimension = dimensionOf(typeKey);
    if (symbolSetDimension != BattleDimension.OTHER && symbolSetDimension != dimension) {
      return Optional.empty();
    }
    CotType byDimension = CotType.of(affiliation, dimension, FunctionKey.empty());
    if (typeKey.length() == 1 || registry == null) {
      return Optional.of(byDimension);
    }
    return Optional.of(registry.find(affiliation, typeKey).orElse(byDimension));
  }

  /**
   * The specialization's enum value: its {@code $discriminator} with or without the
   * {@code NodeSpecializationTypeEnum_} prefix, else the name of the one object it carries
   * ({@code surface_vessel}).
   */
  private static String specializationName(Map<String, Object> specialization) {
    if (specialization == null || specialization.isEmpty()) {
      return null;
    }
    Object discriminator = specialization.get(DISCRIMINATOR);
    if (discriminator != null) {
      String name = String.valueOf(discriminator).trim().toUpperCase(Locale.ROOT);
      return name.startsWith(SPECIALIZATION_PREFIX) ? name.substring(SPECIALIZATION_PREFIX.length()) : name;
    }
    for (String key : specialization.keySet()) {
      if (key != null && !key.startsWith("$")) {
        return key.trim().toUpperCase(Locale.ROOT);
      }
    }
    return null;
  }

  /** The type of the description's 2525D symbol, when it carries an entity code the table maps. */
  private Optional<CotType> entitySymbol(Affiliation affiliation, Map<String, Object> description) {
    if (registry == null || description == null) {
      return Optional.empty();
    }
    Object symbolSet = firstValue(description, "symbol_set", "symbolSet");
    Object entity = firstValue(description, "entity");
    if (symbolSet == null || entity == null) {
      return Optional.empty();
    }
    return registry.fromSymbol2525D(affiliation, String.valueOf(symbolSet), String.valueOf(entity),
        text(firstValue(description, "entity_type", "entityType")),
        text(firstValue(description, "entity_subtype", "entitySubtype")));
  }

  private static String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private CotType vehicleType(Affiliation affiliation, VehicleClass vehicleClass) {
    String typeKey =
        switch (vehicleClass) {
          case UAV -> "A-M-F-Q"; // unmanned aerial vehicle
          case USV -> "S-C-U"; // unmanned surface water vehicle
          case UUV -> "U-S-U"; // autonomous / unmanned underwater vehicle
          case UGV -> "G-U-C-V-U"; // unmanned systems
          case GCS -> "G-U-C";
          case UNKNOWN -> "X";
        };
    Optional<CotType> known = registry == null ? Optional.empty() : registry.find(affiliation, typeKey);
    return known.orElseGet(() -> CotType.of(affiliation, dimensionOf(typeKey), FunctionKey.empty()));
  }

  private static BattleDimension dimensionOf(String typeKey) {
    return BattleDimension.fromCode(typeKey.charAt(0));
  }

  /**
   * The battle dimension of a STANAG 4817 symbol set: the enum name with or without its
   * {@code SymbolSetEnum_} prefix, or the two-digit 2525D code.
   */
  static BattleDimension symbolSetDimension(Map<String, Object> description) {
    Object value = description == null ? null : firstValue(description, "symbol_set", "symbolSet");
    if (value == null) {
      return BattleDimension.OTHER;
    }
    String normalised = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    if (normalised.startsWith(SYMBOL_SET_PREFIX)) {
      normalised = normalised.substring(SYMBOL_SET_PREFIX.length());
    }
    // By family, not by exact name: sources spell the same set differently (LAND_INSTALLATION
    // and LAND_INSTALLATIONS both occur on a live COP).
    if (normalised.startsWith("AIR")) {
      return BattleDimension.AIR;
    }
    if (normalised.startsWith("SPACE")) {
      return BattleDimension.SPACE;
    }
    if (normalised.startsWith("LAND_") || normalised.startsWith("DISMOUNTED")) {
      return BattleDimension.GROUND;
    }
    if (normalised.startsWith("SEA_SUBSURFACE") || normalised.startsWith("MINE_WARFARE")) {
      return BattleDimension.SUBSURFACE;
    }
    if (normalised.startsWith("SEA_SURFACE")) {
      return BattleDimension.SEA_SURFACE;
    }
    return switch (normalised) {
      case "01", "02" -> BattleDimension.AIR;
      case "05", "06" -> BattleDimension.SPACE;
      case "10", "11", "15", "20", "27" -> BattleDimension.GROUND;
      case "30" -> BattleDimension.SEA_SURFACE;
      case "35", "36" -> BattleDimension.SUBSURFACE;
      default -> BattleDimension.OTHER;
    };
  }

  private static Object firstValue(Map<String, Object> values, String... keys) {
    for (String key : keys) {
      Object value = values.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  /** The bundled table, loaded once; absent if it cannot be read, in which case types go unvalidated. */
  private static final class Registry {
    private static final CotTypeRegistry INSTANCE = load();

    private static CotTypeRegistry load() {
      try {
        return CotTypeRegistry.loadDefault();
      } catch (RuntimeException e) {
        return null;
      }
    }
  }
}
