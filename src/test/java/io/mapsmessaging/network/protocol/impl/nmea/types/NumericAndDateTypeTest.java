package io.mapsmessaging.network.protocol.impl.nmea.types;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NumericAndDateTypeTest {

  @Test
  void longTypeHandlesBlankIntegerAndDecimalInput() {
    assertEquals(0L, new LongType("").getValue());
    assertEquals(42L, new LongType("42").getValue());
    assertEquals(42L, new LongType("42.99").getValue());
    assertEquals("-7", new LongType("-7").toString());
    assertEquals(-7L, new LongType("-7").jsonPack());
  }

  @Test
  void longTypeRejectsMalformedNumbers() {
    assertThrows(NumberFormatException.class, () -> new LongType("forty-two"));
  }

  @Test
  void doubleTypeHandlesBlankNormalAndSpecialValues() {
    assertEquals(0.0, new DoubleType("").getValue(), 0.0);
    assertEquals(12.5, new DoubleType("12.5").getValue(), 0.0);
    assertTrue(Double.isNaN(new DoubleType("NaN").getValue()));
    assertEquals("12.5", new DoubleType("12.5").toString());
    assertEquals(12.5, (Double) new DoubleType("12.5").jsonPack(), 0.0);
  }

  @Test
  void doubleTypeRejectsMalformedNumbers() {
    assertThrows(NumberFormatException.class, () -> new DoubleType("not-a-double"));
  }

  @Test
  void dateTypeParsesNmeaDateAndRejectsImpossibleDate() {
    DateType date = new DateType("190926");

    assertEquals(LocalDate.of(2026, 9, 19), date.getDate());
    assertEquals("2026-09-19", date.toString());
    assertEquals("2026-09-19", date.jsonPack());

    assertThrows(RuntimeException.class, () -> new DateType("310226"));
  }
}
