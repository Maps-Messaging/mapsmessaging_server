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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.CloudEventJsonTransformationDTO;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.api.transformers.TransformationTestSupport.utf8Bytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudEventJsonTransformationTest extends AbstractCloudEventTransformationTest {

  @Override
  protected TransformationConfigDTO getConfig() {
    return new CloudEventJsonTransformationDTO();
  }

  @Override
  protected byte[] validInputBytes() {
    return utf8Bytes(TransformationTestVectors.VALID_JSON_OBJECT);
  }

  @Test
  void transform_validJsonObject_producesCloudEventWithDataObject() {
    byte[] before = validInputBytes();

    ParsedMessage result = transformExpectingCloudEvent(before);
    JsonObject ce = parseCloudEventObject(result);

    assertRequiredCloudEventAttributes(ce);

    JsonObject data = assertHasDataObject(ce);
    assertMapsDataIfPresentIsObject(data);
  }

  @Test
  void transform_jsonArray_wrapsUnderPayloadKey() {
    byte[] before = utf8Bytes(TransformationTestVectors.VALID_JSON_OBJECT);

    ParsedMessage result = transformExpectingCloudEvent(before);
    JsonObject ce = parseCloudEventObject(result);

    assertRequiredCloudEventAttributes(ce);

    JsonObject data = assertHasDataObject(ce);
    JsonObject expected = JsonParser.parseString(TransformationTestVectors.VALID_JSON_OBJECT).getAsJsonObject();

    assertEquals(expected, data);


    assertMapsDataIfPresentIsObject(data);
  }

  @Test
  void transform_invalidJson_fallsBackToPayloadBase64Wrapper() {
    byte[] before = utf8Bytes(TransformationTestVectors.INVALID_JSON);

    ParsedMessage result = transformExpectingCloudEvent(before);
    JsonObject ce = parseCloudEventObject(result);

    assertRequiredCloudEventAttributes(ce);

    JsonObject data = assertHasDataObject(ce);
    assertHasPayloadBase64Wrapper(data);
    assertMapsDataIfPresentIsObject(data);
  }
}
