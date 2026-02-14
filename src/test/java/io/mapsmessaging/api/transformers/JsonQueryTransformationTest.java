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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.mapsmessaging.api.transformers.TransformationAssertions.*;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.*;


class JsonQueryTransformationTest extends AbstractDroppingTransformationTest {

  @Override
  protected InterServerTransformation createTransformer() {
    return new JsonQueryTransformation();
  }

  private void setMessageFormatter(InterServerTransformation transformation) {
    try {
      MessageFormatter messageFormatter = MessageFormatterFactory.getInstance().getFormatter(AbstractInterServerTransformationTest.JSON_SCHEMA_CONFIG);
      ((JsonQueryTransformation) transformation).schemaMap.put(AbstractInterServerTransformationTest.SOURCE, messageFormatter);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected byte[] validInputBytes() {
    return utf8Bytes("{\"a\":1,\"b\":\"x\",\"nested\":{\"c\":true}}");
  }

  @Test
  void build_withJsonFormatQuery_extractsValue() {
    ConfigurationProperties props = new ConfigurationProperties();
    // JSON-format query (array form)
    props.put("query", "[\"get\",\"a\"]");

    InterServerTransformation transformer = new JsonQueryTransformation().build(props);
    setMessageFormatter(transformer);

    ParsedMessage result = transformWith(transformer, utf8Bytes("{\"a\":1,\"b\":\"x\"}"));

    assertNotDropped(result);
    assertOpaqueDataEqualsUtf8(result, "1");
  }

  @Test
  void build_withTextFormatQuery_extractsValue() {
    ConfigurationProperties props = new ConfigurationProperties();
    // Text-format query (jsonquerylang text syntax)
    props.put("query", ".a");

    InterServerTransformation transformer = new JsonQueryTransformation().build(props);
    setMessageFormatter(transformer);
    ParsedMessage result = transformWith(transformer, utf8Bytes("{\"a\":1,\"b\":\"x\"}"));

    assertNotDropped(result);
    assertOpaqueDataEqualsUtf8(result, "1");
  }

  @Test
  void build_withBlankQuery_isTrueNoOp() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("query", "   ");

    InterServerTransformation transformer = new JsonQueryTransformation().build(props);
    setMessageFormatter(transformer);

    byte[] before = utf8Bytes("{\"a\":1,\"b\":\"x\"}");
    ParsedMessage result = transformWith(transformer, before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void defaultInstance_programNull_isNoOp() {
    byte[] before = utf8Bytes("{\"a\":1,\"b\":\"x\"}");

    ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void metadata_isStable() {
    InterServerTransformation created = new JsonQueryTransformation();
    assertEquals("JsonQuery", created.getName());
    assertNotNull(created.getDescription());
    assertFalse(created.getDescription().isBlank());
  }

  private ParsedMessage transformWith(InterServerTransformation transformer, byte[] opaqueData) {
    // Force this test instance to run using the built transformer
    this.transformer = transformer;
    return transform(opaqueData);
  }
}
