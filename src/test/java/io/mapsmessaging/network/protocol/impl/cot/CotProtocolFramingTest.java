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

package io.mapsmessaging.network.protocol.impl.cot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CotProtocolFramingTest {

  @Test
  void indexOfHonoursStartOffsetAndMissingPatterns() throws Exception {
    byte[] data = "xx<event>a</event>yy<event>b</event>".getBytes(StandardCharsets.US_ASCII);
    byte[] event = "<event".getBytes(StandardCharsets.US_ASCII);
    Method indexOf = CotProtocol.class.getDeclaredMethod("indexOf", byte[].class, byte[].class, int.class);
    indexOf.setAccessible(true);

    assertEquals(2, indexOf.invoke(null, data, event, -10));
    assertEquals(20, indexOf.invoke(null, data, event, 3));
    assertEquals(-1, indexOf.invoke(null, data, "missing".getBytes(StandardCharsets.US_ASCII), 0));
  }

  @Test
  void lastIndexOfFindsNearestDeclarationWithinBounds() throws Exception {
    byte[] data = "<?xml version='1.0'?> <event/> <?xml version='1.0'?> <event/>"
        .getBytes(StandardCharsets.US_ASCII);
    byte[] xml = "<?xml".getBytes(StandardCharsets.US_ASCII);
    Method lastIndexOf =
        CotProtocol.class.getDeclaredMethod("lastIndexOf", byte[].class, byte[].class, int.class, int.class);
    lastIndexOf.setAccessible(true);

    int secondEvent = new String(data, StandardCharsets.US_ASCII).lastIndexOf("<event");
    int secondDeclaration = new String(data, StandardCharsets.US_ASCII).lastIndexOf("<?xml");

    assertEquals(secondDeclaration, lastIndexOf.invoke(null, data, xml, 0, secondEvent));
    assertEquals(0, lastIndexOf.invoke(null, data, xml, 0, secondDeclaration));
    assertEquals(-1, lastIndexOf.invoke(null, data, xml, secondDeclaration + 1, secondEvent));
  }

  @Test
  void whitespaceDetectionRejectsNonWhitespace() throws Exception {
    Method whitespaceOnly =
        CotProtocol.class.getDeclaredMethod("isWhitespaceOnly", byte[].class, int.class, int.class);
    whitespaceOnly.setAccessible(true);

    byte[] whitespace = " \t\r\n".getBytes(StandardCharsets.US_ASCII);
    byte[] mixed = " \tX\n".getBytes(StandardCharsets.US_ASCII);

    assertEquals(true, whitespaceOnly.invoke(null, whitespace, 0, whitespace.length));
    assertEquals(false, whitespaceOnly.invoke(null, mixed, 0, mixed.length));
  }

  @Test
  void xmlDeclarationIsIncludedOnlyWhenItDirectlyPrecedesEvent() throws Exception {
    CotProtocol protocol = mock(CotProtocol.class);
    Method method = CotProtocol.class.getDeclaredMethod(
        "findPrecedingXmlDeclaration",
        byte[].class,
        int.class,
        int.class
    );
    method.setAccessible(true);

    byte[] valid = "<?xml version='1.0'?>\n  <event></event>".getBytes(StandardCharsets.US_ASCII);
    int validEvent = new String(valid, StandardCharsets.US_ASCII).indexOf("<event");
    assertEquals(0, method.invoke(protocol, valid, validEvent, 0));

    byte[] invalid = "<?xml version='1.0'?>garbage<event></event>".getBytes(StandardCharsets.US_ASCII);
    int invalidEvent = new String(invalid, StandardCharsets.US_ASCII).indexOf("<event");
    assertEquals(invalidEvent, method.invoke(protocol, invalid, invalidEvent, 0));

    byte[] noDeclaration = "<event></event>".getBytes(StandardCharsets.US_ASCII);
    assertEquals(0, method.invoke(protocol, noDeclaration, 0, 0));
  }
}
