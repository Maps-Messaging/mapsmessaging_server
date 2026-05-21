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
import io.mapsmessaging.engine.transformers.TransformerManager;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.api.transformers.TransformationAssertions.*;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.*;

class JsonToValueTransformationTest extends AbstractInPlaceTransformationTest {

  private static ConfigurationProperties config(String transformerName, String... kvPairs) {
    ConfigurationProperties parameters = new ConfigurationProperties();
    for (int i = 0; i < kvPairs.length; i += 2) {
      parameters.put(kvPairs[i], kvPairs[i + 1]);
    }

    ConfigurationProperties root = new ConfigurationProperties();
    root.put("name", transformerName);
    root.put("parameters", parameters);
    return root;
  }

  @Override
  protected InterServerTransformation createTransformer() {
    // default constructor => jsonParser == null => should always pass-through unchanged
    return new JsonToValueTransformation();
  }

  @Override
  protected byte[] validInputBytes() {
    return utf8Bytes(TransformationTestVectors.VALID_JSON_OBJECT);
  }

  @Test
  void transform_defaultInstance_passesThroughUnchanged() {
    byte[] before = utf8Bytes("{\"a\":1,\"b\":\"x\"}");

    ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void build_withKey_extractsValue() {
    InterServerTransformation built = TransformerManager.getInstance().get(config(
        "JsonToValue",
        "key", "b"
    ));

    ParsedMessage result = transformWith(built, utf8Bytes("{\"a\":1,\"b\":\"x\"}"));

    assertNotDropped(result);
    assertOpaqueDataEqualsUtf8(result, "x");
  }

  @Test
  void build_withData_fallback_extractsValue() {
    InterServerTransformation built = TransformerManager.getInstance().get(config(
        "JsonToValue",
        "data", "a"
    ));

    ParsedMessage result = transformWith(built, utf8Bytes("{\"a\":1,\"b\":\"x\"}"));

    assertNotDropped(result);
    assertOpaqueDataEqualsUtf8(result, "1");
  }

  @Test
  void configuredKey_missingValue_passesThroughUnchanged() {
    InterServerTransformation built = TransformerManager.getInstance().get(config(
        "JsonToValue",
        "key", "nope"
    ));

    byte[] before = utf8Bytes("{\"a\":1,\"b\":\"x\"}");
    ParsedMessage result = transformWith(built, before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void invalidJson_passesThroughUnchanged() {
    InterServerTransformation built = TransformerManager.getInstance().get(config(
        "JsonToValue",
        "key", "a"
    ));

    byte[] before = utf8Bytes(TransformationTestVectors.INVALID_JSON);
    ParsedMessage result = transformWith(built, before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void metadata_isStable() {
    InterServerTransformation created = createTransformer();
    assertEquals("JsonToValue", created.getName());
    assertNotNull(created.getDescription());
    assertFalse(created.getDescription().isBlank());
  }

  private ParsedMessage transformWith(InterServerTransformation transformer, byte[] opaqueData) {
    this.transformer = transformer;
    return transform(opaqueData);
  }
}
