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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.api.transformers.TransformationAssertions.assertNotDropped;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;

public abstract class AbstractInPlaceTransformationTest extends AbstractInterServerTransformationTest {

  protected abstract byte[] validInputBytes();

  @Test
  void transform_validInput_doesNotDrop() {
    Protocol.ParsedMessage result = transform(validInputBytes());
    assertNotDropped(result);
  }

  @Test
  void transform_invalidInput_doesNotThrow() {
    Protocol.ParsedMessage result = transform(utf8Bytes(TransformationTestVectors.NON_JSON_TEXT));
    // may or may not change payload, but must not crash
    // For in-place transformers, we expect not dropped.
    assertNotDropped(result);
  }
}
