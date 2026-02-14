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

import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.api.transformers.TransformationAssertions.*;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.*;

class JSONToXMLTest extends AbstractInPlaceTransformationTest {

  @Override
  protected InterServerTransformation createTransformer() {
    return new JSONToXML();
  }

  @Override
  protected byte[] validInputBytes() {
    return utf8Bytes(TransformationTestVectors.VALID_JSON_OBJECT);
  }

  @Test
  void transform_validJsonObject_producesXml() {
    byte[] before = validInputBytes();

    ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataChanged(before, result);
    assertOpaqueDataIsXml(result);
  }

  @Test
  void transform_invalidJson_doesNotDrop_andLeavesPayloadUnchanged() {
    byte[] before = utf8Bytes(TransformationTestVectors.INVALID_JSON);

    ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void transform_jsonArray_doesNotDrop_andLeavesPayloadUnchanged() {
    // getAsJsonObject() will throw for arrays, caught by convert()
    byte[] before = utf8Bytes(TransformationTestVectors.VALID_JSON_ARRAY);

    ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataUnchanged(before, result);
  }

  @Test
  void build_returnsSameInstance() {
    InterServerTransformation created = createTransformer();
    InterServerTransformation built = created.build(TransformationTestSupport.emptyProperties());
    assertSame(created, built);
  }

  @Test
  void metadata_isStable() {
    InterServerTransformation created = createTransformer();
    assertEquals("JSONToXML", created.getName());
    assertNotNull(created.getDescription());
    assertFalse(created.getDescription().isBlank());
  }
}
