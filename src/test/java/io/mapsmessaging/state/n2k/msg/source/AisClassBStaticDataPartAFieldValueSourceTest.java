package io.mapsmessaging.state.n2k.msg.source;

import io.mapsmessaging.state.n2k.msg.AisClassBStaticDataPartAReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisClassBStaticDataPartAFieldValueSourceTest {

  @Test
  void exposesAllPopulatedStaticPartAFields() {
    AisClassBStaticDataPartAReport report =
        new AisClassBStaticDataPartAReport(
            24L,
            1L,
            123456789L,
            "VESSEL",
            2L,
            7L
        );

    AisClassBStaticDataPartAFieldValueSource source =
        new AisClassBStaticDataPartAFieldValueSource(report);

    assertEquals(24L, source.getLong("messageId"));
    assertEquals(1L, source.getLong("repeatIndicator"));
    assertEquals(123456789L, source.getLong("userId"));
    assertEquals("VESSEL", source.getString("name"));
    assertEquals(2L, source.getLong("aisTransceiverInformation"));
    assertEquals(7L, source.getLong("sequenceId"));
    assertTrue(source.has("name"));
  }

  @Test
  void nullOrEmptyOptionalValuesAreNotFabricated() {
    AisClassBStaticDataPartAReport report =
        new AisClassBStaticDataPartAReport(
            24L,
            null,
            123L,
            "",
            null,
            null
        );

    AisClassBStaticDataPartAFieldValueSource source =
        new AisClassBStaticDataPartAFieldValueSource(report);

    assertFalse(source.has("repeatIndicator"));
    assertFalse(source.has("name"));
    assertFalse(source.has("sequenceId"));
    assertNull(source.getString("name"));
  }
}
