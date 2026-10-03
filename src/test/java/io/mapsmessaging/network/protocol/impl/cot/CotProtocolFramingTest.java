/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.cot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CotProtocolFramingTest {

  @ParameterizedTest
  @MethodSource("indexCases")
  void indexOfHandlesOffsetsAndPatterns(String source, String pattern, int from, int expected)
      throws Exception {
    assertEquals(expected, indexOf(source, pattern, from));
  }

  @ParameterizedTest
  @MethodSource("lastIndexCases")
  void lastIndexOfHonoursBounds(String source, String pattern, int from, int to, int expected)
      throws Exception {
    byte[] data = ascii(source);
    Method method = CotProtocol.class.getDeclaredMethod(
        "lastIndexOf", byte[].class, byte[].class, int.class, int.class);
    method.setAccessible(true);

    assertEquals(expected, method.invoke(null, data, ascii(pattern), from, to));
  }

  @ParameterizedTest
  @CsvSource({
      "'',0,0,true",
      "' ',0,1,true",
      "'\t',0,1,true",
      "'\r\n',0,2,true",
      "' \t\r\n',0,4,true",
      "'X',0,1,false",
      "' X ',0,3,false",
      "'abc',1,1,true",
      "'a b',1,2,true"
  })
  void whitespaceDetectionHandlesRanges(String source, int from, int to, boolean expected)
      throws Exception {
    Method method = CotProtocol.class.getDeclaredMethod(
        "isWhitespaceOnly", byte[].class, int.class, int.class);
    method.setAccessible(true);

    assertEquals(expected, method.invoke(null, ascii(source), from, to));
  }

  @ParameterizedTest
  @MethodSource("declarationCases")
  void xmlDeclarationSelectionFollowsDirectPrecedenceRule(
      String source, int lowerBound, int expectedStart) throws Exception {
    byte[] data = ascii(source);
    int eventStart = source.indexOf("<event");
    CotProtocol protocol = mock(CotProtocol.class);
    Method method = CotProtocol.class.getDeclaredMethod(
        "findPrecedingXmlDeclaration", byte[].class, int.class, int.class);
    method.setAccessible(true);

    assertEquals(expectedStart, method.invoke(protocol, data, eventStart, lowerBound));
  }

  @Test
  void indexOfHonoursNegativeStartAndMissingPattern() throws Exception {
    String source = "xx<event>a</event>yy<event>b</event>";
    assertEquals(2, indexOf(source, "<event", -10));
    assertEquals(20, indexOf(source, "<event", 3));
    assertEquals(-1, indexOf(source, "missing", 0));
  }

  private int indexOf(String source, String pattern, int from) throws Exception {
    Method method = CotProtocol.class.getDeclaredMethod(
        "indexOf", byte[].class, byte[].class, int.class);
    method.setAccessible(true);
    return (int) method.invoke(null, ascii(source), ascii(pattern), from);
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }

  private static Stream<Arguments> indexCases() {
    return Stream.of(
        Arguments.of("<event></event>", "<event", 0, 0),
        Arguments.of("xx<event></event>", "<event", 0, 2),
        Arguments.of("xx<event></event>", "<event", 2, 2),
        Arguments.of("xx<event></event>", "<event", 3, -1),
        Arguments.of("<event></event><event></event>", "<event", 1, 15),
        Arguments.of("abcabc", "abc", 0, 0),
        Arguments.of("abcabc", "abc", 1, 3),
        Arguments.of("abcabc", "abc", 4, -1),
        Arguments.of("abc", "abcd", 0, -1),
        Arguments.of("", "a", 0, -1),
        Arguments.of("aaaa", "aa", 0, 0),
        Arguments.of("aaaa", "aa", 1, 1)
    );
  }

  private static Stream<Arguments> lastIndexCases() {
    String source = "<?xml?> <event/> <?xml?> <event/>";
    return Stream.of(
        Arguments.of(source, "<?xml", 0, source.length(), 17),
        Arguments.of(source, "<?xml", 0, 17, 0),
        Arguments.of(source, "<?xml", 1, source.length(), 17),
        Arguments.of(source, "<?xml", 18, source.length(), -1),
        Arguments.of("abcabc", "abc", 0, 6, 3),
        Arguments.of("abcabc", "abc", 0, 3, 0),
        Arguments.of("abcabc", "abc", 4, 6, -1),
        Arguments.of("abc", "missing", 0, 3, -1)
    );
  }

  private static Stream<Arguments> declarationCases() {
    return Stream.of(
        Arguments.of("<?xml version='1.0'?><event></event>", 0, 0),
        Arguments.of("<?xml version='1.0'?> <event></event>", 0, 0),
        Arguments.of("<?xml version='1.0'?>\n\t<event></event>", 0, 0),
        Arguments.of("<?xml version='1.0'?>garbage<event></event>", 0, 27),
        Arguments.of("<event></event>", 0, 0),
        Arguments.of("noise<?xml version='1.0'?> <event></event>", 0, 5),
        Arguments.of("<?xml version='1.0'?> <event></event>", 1, 22),
        Arguments.of("<?xml version='1.0'<event></event>", 0, 20),
        Arguments.of("<?xml?>X<event></event>", 0, 8)
    );
  }
}
