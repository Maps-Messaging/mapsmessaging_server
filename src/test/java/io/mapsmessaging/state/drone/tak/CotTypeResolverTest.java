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

  // --- the STANAG 4817 node specialization -------------------------------------------------
  // A source that classifies its node but sends no 2525D entity code still says what kind of
  // platform it is. That is worth a function, but never worth contradicting the symbol set.

  @ParameterizedTest
  @MethodSource("specializations")
  void aSpecializationTypesATwinTheEntityCodeDoesNot(String symbolSet, String type, String expectedType) {
    assertEquals(expectedType,
        resolver.resolve(Affiliation.FRIEND, null, description(symbolSet), specialization(type)));
  }

  @Test
  void theSpecializationIsReadFromTheObjectKeyWhenTheDiscriminatorIsMissing() {
    Map<String, Object> specialization = new HashMap<>();
    specialization.put("surface_unmanned_system", Map.of("app11_vessel_type", "NavalVesselTypeEnum_NOT_OTHERWISE_SPECIFIED"));
    assertEquals("a-f-S-C-U",
        resolver.resolve(Affiliation.FRIEND, null, description("SymbolSetEnum_SEA_SURFACE"), specialization));
  }

  @Test
  void theEntityCodeWinsOverTheSpecialization() {
    // the code is the finer statement: a vessel that also declares itself unmanned
    Map<String, Object> description = description("SymbolSetEnum_SEA_SURFACE");
    description.put("entity", "12");
    description.put("entity_type", "07");
    description.put("entity_subtype", "00");
    assertEquals("a-f-S-C-U",
        resolver.resolve(Affiliation.FRIEND, null, description, specialization("SURFACE_VESSEL")));
  }

  @Test
  void theVehicleClassWinsOverTheSpecialization() {
    assertEquals("a-f-S-C-U",
        resolver.resolve(Affiliation.FRIEND, VehicleClass.USV, description("SymbolSetEnum_AIR"), specialization("AIRCRAFT")));
  }

  @Test
  void aSpecializationOutsideTheSymbolSetsDimensionIsIgnored() {
    // the symbol set is the 2525 statement; a specialization that disagrees is a source error,
    // and moving a sea track into the air dimension is worse than saying less about it
    assertEquals("a-f-S",
        resolver.resolve(Affiliation.FRIEND, null, description("SymbolSetEnum_SEA_SURFACE"), specialization("AIRCRAFT")));
  }

  @Test
  void aSpecializationTypesATwinWithNoDescriptionAtAll() {
    assertEquals("a-f-S-C-U", resolver.resolve(Affiliation.FRIEND, null, null, specialization("SURFACE_UNMANNED_SYSTEM")));
    assertEquals("a-f-U-S-U", resolver.resolve(Affiliation.FRIEND, null, Map.of(), specialization("SUBSURFACE_UNMANNED_SYSTEM")));
  }

  @ParameterizedTest
  @MethodSource("unmappedSpecializations")
  void aSpecializationTheTableCannotPlaceChangesNothing(String type) {
    assertEquals("a-f-S", resolver.resolve(Affiliation.FRIEND, null, description("SEA_SURFACE"), specialization(type)));
    assertEquals("a-f-X", resolver.resolve(Affiliation.FRIEND, null, null, specialization(type)));
  }

  @Test
  void withoutTheTableTheSpecializationKeepsItsDimension() {
    CotTypeResolver unvalidated = new CotTypeResolver(null);
    assertEquals("a-f-S", unvalidated.resolve(Affiliation.FRIEND, null, null, specialization("SURFACE_UNMANNED_SYSTEM")));
    assertEquals("a-f-G", unvalidated.resolve(Affiliation.FRIEND, null, null, specialization("RADAR")));
  }

  private static Map<String, Object> specialization(String type) {
    Map<String, Object> specialization = new HashMap<>();
    specialization.put("$discriminator", "NodeSpecializationTypeEnum_" + type);
    specialization.put(type.toLowerCase(java.util.Locale.ROOT), new HashMap<>());
    return specialization;
  }

  private static Stream<Arguments> specializations() {
    return Stream.of(
        // the three unmanned systems the table knows by name
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "SURFACE_UNMANNED_SYSTEM", "a-f-S-C-U"),
        Arguments.of("SymbolSetEnum_SEA_SUBSURFACE", "SUBSURFACE_UNMANNED_SYSTEM", "a-f-U-S-U"),
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "GROUND_UNMANNED_SYSTEM", "a-f-G-U-C-V-U"),
        // a crewed vessel: the dimension is all the specialization claims
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "SURFACE_VESSEL", "a-f-S"),
        Arguments.of("SymbolSetEnum_SEA_SURFACE", "VESSEL", "a-f-S"),
        Arguments.of("SymbolSetEnum_SEA_SUBSURFACE", "SUBSURFACE_VESSEL", "a-f-U"),
        Arguments.of("SymbolSetEnum_AIR", "AIRCRAFT", "a-f-A"),
        Arguments.of("SPACE", "SPACECRAFT", "a-f-P"),
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "LAND_VEHICLE", "a-f-G"),
        // sensors: the table has one sensor, one radar and CBRN equipment
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "RADAR", "a-f-G-E-S-R"),
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "SONAR", "a-f-G-E-S"),
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "ACOUSTIC_SENSOR", "a-f-G-E-S"),
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "SENSOR", "a-f-G-E-S"),
        Arguments.of("SymbolSetEnum_LAND_EQUIPMENT", "CBRN_SENSOR", "a-f-G-E-X-N"));
  }

  private static Stream<Arguments> unmappedSpecializations() {
    // a value the table cannot place says nothing more than the symbol set already did
    return Stream.of(Arguments.of("OTHER"), Arguments.of("VEHICLE"), Arguments.of("CYBER_SENSOR"));
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
