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
 * has one (MAVLink vehicles); the STANAG 4817 {@code symbol_set} of its description (COP twins);
 * otherwise the "other" dimension. A vehicle-class type is checked against the table and falls
 * back to its dimension alone when the table does not carry it.
 */
final class CotTypeResolver {

  private static final String SYMBOL_SET_PREFIX = "SYMBOLSETENUM_";

  private final CotTypeRegistry registry;

  CotTypeResolver() {
    this(Registry.INSTANCE);
  }

  CotTypeResolver(CotTypeRegistry registry) {
    this.registry = registry;
  }

  String resolve(Affiliation affiliation, VehicleClass vehicleClass, Map<String, Object> description) {
    Affiliation resolvedAffiliation = affiliation == null ? Affiliation.UNKNOWN : affiliation;
    if (vehicleClass != null && vehicleClass != VehicleClass.UNKNOWN) {
      return vehicleType(resolvedAffiliation, vehicleClass).toString();
    }
    return CotType.of(resolvedAffiliation, symbolSetDimension(description), FunctionKey.empty()).toString();
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
    return switch (normalised) {
      case "AIR", "AIR_MISSILE", "01", "02" -> BattleDimension.AIR;
      case "SPACE", "SPACE_MISSILE", "05", "06" -> BattleDimension.SPACE;
      case "LAND_UNIT", "LAND_CIVILIAN_UNIT_ORGANIZATION", "LAND_EQUIPMENT", "LAND_INSTALLATION",
          "DISMOUNTED_INDIVIDUAL", "10", "11", "15", "20", "27" -> BattleDimension.GROUND;
      case "SEA_SURFACE", "30" -> BattleDimension.SEA_SURFACE;
      case "SEA_SUBSURFACE", "MINE_WARFARE", "35", "36" -> BattleDimension.SUBSURFACE;
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
