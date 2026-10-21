package io.mapsmessaging.tools.config.lint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringEnumHeuristicsTest {

  @Test
  void nullBlankAndOpenVocabularyNamesAreNotEnums() {
    assertFalse(StringEnumHeuristics.looksLikeEnum(null));
    assertFalse(StringEnumHeuristics.looksLikeEnum(""));
    assertFalse(StringEnumHeuristics.looksLikeEnum("   "));
    assertFalse(StringEnumHeuristics.looksLikeEnum("contentType"));
    assertFalse(StringEnumHeuristics.looksLikeEnum("mediaType"));
    assertFalse(StringEnumHeuristics.looksLikeEnum("mimeType"));
  }

  @Test
  void exactSuffixAndEmbeddedTokensAreDetectedCaseInsensitively() {
    assertTrue(StringEnumHeuristics.looksLikeEnum("mode"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("flightMode"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("connectionStatusText"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("AUTH_POLICY"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("MessageFormat"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("transportProtocol"));
  }

  @Test
  void ordinaryFreeTextNamesAreNotEnums() {
    assertFalse(StringEnumHeuristics.looksLikeEnum("description"));
    assertFalse(StringEnumHeuristics.looksLikeEnum("hostname"));
    assertFalse(StringEnumHeuristics.looksLikeEnum("displayName"));
  }
}
