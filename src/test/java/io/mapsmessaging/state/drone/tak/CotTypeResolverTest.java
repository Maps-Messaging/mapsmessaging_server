package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.cot.types.Affiliation;
import io.mapsmessaging.state.config.VehicleClass;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CotTypeResolverTest {

  private final CotTypeResolver resolver = new CotTypeResolver();

  @ParameterizedTest
  @MethodSource("symbolSets")
  void copTwinDimensionFollowsTheSymbolSetInEverySpelling(String symbolSet, String expectedType) {
    assertEquals(expectedType, resolver.resolve(Affiliation.NEUTRAL, null, description(symbolSet)));
  }

  @Test
  void aTwinWithoutADescriptionIsTypedOther() {
    assertEquals("a-u-X", resolver.resolve(Affiliation.UNKNOWN, null, null));
    assertEquals("a-u-X", resolver.resolve(Affiliation.UNKNOWN, VehicleClass.UNKNOWN, Map.of()));
  }

  @Test
  void aMissingAffiliationIsUnknown() {
    assertEquals("a-u-S-C-U", resolver.resolve(null, VehicleClass.USV, null));
  }

  @Test
  void theVehicleClassWinsOverTheSymbolSet() {
    // a MAVLink USV whose twin also carries a description
    assertEquals("a-f-S-C-U", resolver.resolve(Affiliation.FRIEND, VehicleClass.USV, description("AIR")));
  }

  @Test
  void withoutTheTableAVehicleKeepsItsDimension() {
    CotTypeResolver unvalidated = new CotTypeResolver(null);
    assertEquals("a-h-S", unvalidated.resolve(Affiliation.HOSTILE, VehicleClass.USV, null));
    assertEquals("a-h-A", unvalidated.resolve(Affiliation.HOSTILE, VehicleClass.UAV, null));
    assertEquals("a-h-U", unvalidated.resolve(Affiliation.HOSTILE, null, description("SEA_SUBSURFACE")));
  }

  private static Map<String, Object> description(String symbolSet) {
    Map<String, Object> description = new HashMap<>();
    description.put("symbol_set", symbolSet);
    return description;
  }

  private static Stream<Arguments> symbolSets() {
    return Stream.of(
        // both spellings seen on a live COP, plus the 2525D two-digit codes
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "a-n-S"),
        Arguments.of("SEA_SURFACE", "a-n-S"),
        Arguments.of("30", "a-n-S"),
        Arguments.of("SymbolSetEnum_AIR", "a-n-A"),
        Arguments.of("air", "a-n-A"),
        Arguments.of("01", "a-n-A"),
        Arguments.of("SymbolSetEnum_SEA_SUBSURFACE", "a-n-U"),
        Arguments.of("MINE_WARFARE", "a-n-U"),
        Arguments.of("SymbolSetEnum_LAND_UNIT", "a-n-G"),
        Arguments.of("LAND_EQUIPMENT", "a-n-G"),
        Arguments.of("SymbolSetEnum_LAND_INSTALLATIONS", "a-n-G"),   // plural, seen live
        Arguments.of("SymbolSetEnum_AIR_MISSILE", "a-n-A"),
        Arguments.of("SPACE", "a-n-P"),
        Arguments.of("SymbolSetEnum_CONTROL_MEASURE", "a-n-X"),
        Arguments.of("", "a-n-X"),
        Arguments.of("not-a-symbol-set", "a-n-X"));
  }
}
