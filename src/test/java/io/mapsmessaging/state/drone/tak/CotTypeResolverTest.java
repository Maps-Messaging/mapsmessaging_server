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

  @ParameterizedTest
  @MethodSource("entityCodes")
  void aDescriptionWithA2525DEntityCodeGetsItsSymbol(String symbolSet, Object entity, Object type, Object subtype,
      Affiliation affiliation, String expectedType) {
    Map<String, Object> description = description(symbolSet);
    description.put("entity", entity);
    description.put("entity_type", type);
    description.put("entity_subtype", subtype);
    assertEquals(expectedType, resolver.resolve(affiliation, null, description));
  }

  @Test
  void anEntityCodeWithNoCounterpartKeepsTheSymbolSetDimension() {
    // Sea Surface 11 00 00 (Military, unspecified) maps to nothing: as before, a-f-S
    Map<String, Object> description = description("SymbolSetEnum_SEA_SURFACE");
    description.put("entity", "11");
    description.put("entity_type", "00");
    description.put("entity_subtype", "00");
    assertEquals("a-f-S", resolver.resolve(Affiliation.FRIEND, null, description));
  }

  @Test
  void theVehicleClassWinsOverTheEntityCode() {
    Map<String, Object> description = description("SymbolSetEnum_AIR");
    description.put("entity", "12");
    assertEquals("a-f-S-C-U", resolver.resolve(Affiliation.FRIEND, VehicleClass.USV, description));
  }

  @Test
  void withoutTheTableTheEntityCodeIsIgnored() {
    Map<String, Object> description = description("SymbolSetEnum_SEA_SURFACE");
    description.put("entity", "12");
    description.put("entity_type", "07");
    assertEquals("a-f-S", new CotTypeResolver(null).resolve(Affiliation.FRIEND, null, description));
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

  private static Stream<Arguments> entityCodes() {
    return Stream.of(
        // classifications seen on a live COP: symbol set, entity, type, subtype
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "12", "07", "00", Affiliation.FRIEND, "a-f-S-C-U"),   // USV
        Arguments.of("SymbolSetEnum_SEA_SUBSURFACE", "11", "04", "00", Affiliation.FRIEND, "a-f-U-S-U"), // UUV
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "15", "00", "00", Affiliation.FRIEND, "a-f-S-O"),     // own ship
        Arguments.of("SymbolSetEnum_AIR", "12", "00", "00", Affiliation.FRIEND, "a-f-A-C"),             // civilian aircraft
        // a military RHIB has no 2525C symbol: its parent, Military Combatant
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "12", "08", "01", Affiliation.FRIEND, "a-f-S-C"),
        // the affiliation is the source's, the symbol the entity code's
        Arguments.of("SEA_SURFACE", "12", "07", "00", Affiliation.NEUTRAL, "a-n-S-C-U"),
        // codes as JSON numbers, and a missing type and subtype
        Arguments.of("SymbolSetEnum_SEA_SURFACE", 12, 7, 0, Affiliation.FRIEND, "a-f-S-C-U"),
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "12", null, null, Affiliation.FRIEND, "a-f-S-C"));
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
