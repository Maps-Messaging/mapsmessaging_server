/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the Apache License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.api.transformers.TransformationAssertions.*;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.*;

class JsonToValueTransformationTest extends AbstractInPlaceTransformationTest {

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

    Protocol.ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void build_withKey_extractsValue() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("key", "b");

    InterServerTransformation built = new JsonToValueTransformation().build(props);

    Protocol.ParsedMessage result = transformWith(built, utf8Bytes("{\"a\":1,\"b\":\"x\"}"));

    assertNotDropped(result);
    assertOpaqueDataEqualsUtf8(result, "x");
  }

  @Test
  void build_withData_fallback_extractsValue() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("data", "a");

    InterServerTransformation built = new JsonToValueTransformation().build(props);

    Protocol.ParsedMessage result = transformWith(built, utf8Bytes("{\"a\":1,\"b\":\"x\"}"));

    assertNotDropped(result);
    assertOpaqueDataEqualsUtf8(result, "1");
  }

  @Test
  void configuredKey_missingValue_passesThroughUnchanged() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("key", "nope");

    InterServerTransformation built = new JsonToValueTransformation().build(props);

    byte[] before = utf8Bytes("{\"a\":1,\"b\":\"x\"}");
    Protocol.ParsedMessage result = transformWith(built, before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void invalidJson_passesThroughUnchanged() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("key", "a");

    InterServerTransformation built = new JsonToValueTransformation().build(props);

    byte[] before = utf8Bytes(TransformationTestVectors.INVALID_JSON);
    Protocol.ParsedMessage result = transformWith(built, before);

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

  private Protocol.ParsedMessage transformWith(InterServerTransformation transformer, byte[] opaqueData) {
    // minimal local helper to avoid depending on base's transformer field
    // and to test build() products cleanly.
    this.transformer = transformer;
    return transform(opaqueData);
  }
}
