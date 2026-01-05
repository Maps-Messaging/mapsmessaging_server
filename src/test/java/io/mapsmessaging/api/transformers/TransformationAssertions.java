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

package io.mapsmessaging.api.transformers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.Protocol;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8String;
import static org.junit.jupiter.api.Assertions.*;

public final class TransformationAssertions {

  private TransformationAssertions() {
  }

  public static void assertNotDropped(Protocol.ParsedMessage result) {
    assertNotNull(result, "Expected message to NOT be dropped (null indicates filtered-out event)");
    assertNotNull(result.getMessage(), "Result ParsedMessage.message must not be null");
  }

  public static void assertDropped(Protocol.ParsedMessage result) {
    assertNull(result, "Expected message to be dropped (null)");
  }

  public static void assertOpaqueDataEqualsUtf8(Protocol.ParsedMessage result, String expected) {
    assertNotDropped(result);
    Message message = result.getMessage();
    byte[] actualBytes = message.getOpaqueData();
    assertNotNull(actualBytes, "opaqueData must not be null");
    assertEquals(expected, new String(actualBytes, StandardCharsets.UTF_8));
  }

  public static void assertOpaqueDataChanged(byte[] before, Protocol.ParsedMessage result) {
    assertNotDropped(result);
    byte[] after = result.getMessage().getOpaqueData();
    assertNotNull(after, "opaqueData must not be null");
    assertNotEquals(utf8String(before), utf8String(after), "Expected opaqueData content to change");
  }

  public static void assertOpaqueDataUnchanged(byte[] before, Protocol.ParsedMessage result) {
    assertNotDropped(result);
    byte[] after = result.getMessage().getOpaqueData();
    assertNotNull(after, "opaqueData must not be null");
    assertArrayEquals(before, after, "Expected opaqueData bytes to remain unchanged");
  }

  public static JsonElement assertOpaqueDataIsJson(Protocol.ParsedMessage result) {
    assertNotDropped(result);
    byte[] bytes = result.getMessage().getOpaqueData();
    assertNotNull(bytes, "opaqueData must not be null");
    assertDoesNotThrow(() -> JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)));
    return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
  }

  public static void assertOpaqueDataIsJsonObject(Protocol.ParsedMessage result) {
    JsonElement element = assertOpaqueDataIsJson(result);
    assertTrue(element.isJsonObject(), "Expected JSON object");
  }

  public static void assertOpaqueDataIsXml(Protocol.ParsedMessage result) {
    assertNotDropped(result);
    byte[] bytes = result.getMessage().getOpaqueData();
    assertNotNull(bytes, "opaqueData must not be null");

    assertDoesNotThrow(() -> {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setExpandEntityReferences(false);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    });
  }
}
