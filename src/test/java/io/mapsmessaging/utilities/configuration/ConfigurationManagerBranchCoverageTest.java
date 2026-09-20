package io.mapsmessaging.utilities.configuration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationManagerBranchCoverageTest {

  @Test
  void unknownManagerAndSchemaLookupsReturnStableEmptyResults() {
    ConfigurationManager manager = ConfigurationManager.getInstance();

    assertNull(manager.getManager("definitely-missing"));
    assertNull(manager.getSchema("definitely-missing"));
    assertEquals(
        java.util.List.of("Schema not found for: definitely-missing"),
        manager.validateConfiguration("definitely-missing", Map.of()));
  }

  @Test
  void registerIsSafeNoOp() {
    assertDoesNotThrow(() -> ConfigurationManager.getInstance().register());
  }
}