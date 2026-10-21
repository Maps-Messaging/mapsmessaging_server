package io.mapsmessaging.tools.config.yaml;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlValueFormatterTest {

  private final YamlValueFormatter formatter = new YamlValueFormatter();

  @Test
  void formatsNullBooleansAndNumbers() {
    assertEquals("null", formatter.formatScalar(null));
    assertEquals("true", formatter.formatScalar(true));
    assertEquals("false", formatter.formatScalar(false));
    assertEquals("42", formatter.formatScalar(42));
    assertEquals("12.5", formatter.formatScalar(12.5d));
    assertEquals("10", formatter.formatScalar(10.0d));
    assertEquals("3.25", formatter.formatScalar(3.25f));
    assertEquals("7", formatter.formatScalar(7.0f));
  }

  @Test
  void quotesYamlLiteralsAndStructurallySensitiveStrings() {
    assertEquals("\"\"", formatter.formatScalar(""));
    assertEquals("\"null\"", formatter.formatScalar("null"));
    assertEquals("\"true\"", formatter.formatScalar("true"));
    assertEquals("\"12.5\"", formatter.formatScalar("12.5"));
    assertEquals("\" leading\"", formatter.formatScalar(" leading"));
    assertEquals("\"trailing \"", formatter.formatScalar("trailing "));
    assertEquals("\"a: b\"", formatter.formatScalar("a: b"));
    assertEquals("\"a#b\"", formatter.formatScalar("a#b"));
    assertEquals("\"-value\"", formatter.formatScalar("-value"));
    assertEquals("\"line1\nline2\"", formatter.formatScalar("line1\nline2"));
  }

  @Test
  void leavesSimpleStringsUnquotedAndEscapesQuotedContent() {
    assertEquals("maps", formatter.formatScalar("maps"));
    assertEquals("\"a\\\\b\\\"c#\"", formatter.formatScalar("a\\b\"c#"));
  }

  @Test
  void rendersCollectionsAsStructuralPlaceholders() {
    assertEquals("{}", formatter.formatScalar(Map.of("a", 1)));
    assertEquals("[]", formatter.formatScalar(List.of(1, 2)));
  }
}
