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

import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.api.transformers.TransformationAssertions.*;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.*;

class XMLToJSONTest extends AbstractInPlaceTransformationTest {


  @Override
  protected InterServerTransformation createTransformer() {
    return new XMLToJSON();
  }

  @Override
  protected byte[] validInputBytes() {
    return utf8Bytes(TransformationTestVectors.VALID_XML_SIMPLE);
  }

  @Test
  void transform_validXml_producesJsonObject() {
    byte[] before = validInputBytes();

    Protocol.ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataChanged(before, result);
    assertOpaqueDataIsJsonObject(result);
  }

  @Test
  void transform_invalidXml_doesNotDrop_andLeavesPayloadUnchanged() {
    byte[] before = utf8Bytes(TransformationTestVectors.INVALID_XML);

    Protocol.ParsedMessage result = transform(before);

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
    assertEquals("XMLToJSON", created.getName());
    assertNotNull(created.getDescription());
    assertFalse(created.getDescription().isBlank());
  }

}
