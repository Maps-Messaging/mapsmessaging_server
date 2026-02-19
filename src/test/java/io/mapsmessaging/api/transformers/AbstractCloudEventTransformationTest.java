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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.engine.transformers.TransformerManager;

import java.nio.charset.StandardCharsets;

import static io.mapsmessaging.api.transformers.TransformationAssertions.assertNotDropped;
import static io.mapsmessaging.api.transformers.TransformationAssertions.assertOpaqueDataChanged;
import static io.mapsmessaging.api.transformers.TransformationAssertions.assertOpaqueDataIsJson;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractCloudEventTransformationTest extends AbstractInPlaceTransformationTest {

  protected static final String MAPS_DATA_KEY = "_mapsData";
  protected static final String PAYLOAD_KEY = "_payload";
  protected static final String PAYLOAD_BASE64_KEY = "payload_base64";
  protected static final String PAYLOAD_MIME_KEY = "payload_mime";

  protected abstract TransformationConfigDTO getConfig();

  protected InterServerTransformation createTransformer() {
    return TransformerManager.getInstance().get(getConfig());
  }

  protected final ParsedMessage transformExpectingCloudEvent(byte[] before) {
    ParsedMessage result = transform(before);

    assertNotDropped(result);
    assertOpaqueDataChanged(before, result);
    assertOpaqueDataIsJson(result);

    return result;
  }

  protected final JsonObject parseCloudEventObject(ParsedMessage result) {
    byte[] bytes = result.getMessage().getOpaqueData();
    assertNotNull(bytes);

    String json = new String(bytes, StandardCharsets.UTF_8);
    JsonElement parsed = JsonParser.parseString(json);

    assertNotNull(parsed);
    assertTrue(parsed.isJsonObject(), "Expected CloudEvent JSON object but got: " + parsed);

    return parsed.getAsJsonObject();
  }

  protected final void assertRequiredCloudEventAttributes(JsonObject ce) {
    assertHasNonBlankString(ce, "specversion");
    assertHasNonBlankString(ce, "id");
    assertHasNonBlankString(ce, "source");
    assertHasNonBlankString(ce, "type");
  }

  protected final JsonObject assertHasDataObject(JsonObject ce) {
    assertTrue(ce.has("data"), "Expected top-level 'data' field");
    JsonElement data = ce.get("data");
    assertNotNull(data);
    assertFalse(data.isJsonNull(), "data is null");
    assertTrue(data.isJsonObject(), "Expected data to be a JSON object but got: " + data);
    return data.getAsJsonObject();
  }

  protected final void assertHasPayloadWrappedValue(JsonObject data) {
    assertTrue(data.has(PAYLOAD_KEY), "Expected data." + PAYLOAD_KEY);
    JsonElement payload = data.get(PAYLOAD_KEY);
    assertNotNull(payload);
    assertFalse(payload.isJsonNull(), "data." + PAYLOAD_KEY + " is null");
  }

  protected final void assertHasPayloadBase64Wrapper(JsonObject data) {
    assertTrue(data.has(PAYLOAD_BASE64_KEY), "Expected data." + PAYLOAD_BASE64_KEY);
    assertTrue(data.has(PAYLOAD_MIME_KEY), "Expected data." + PAYLOAD_MIME_KEY);

    String base64 = data.get(PAYLOAD_BASE64_KEY).getAsString();
    String mime = data.get(PAYLOAD_MIME_KEY).getAsString();

    assertNotNull(base64);
    assertFalse(base64.isBlank());

    assertNotNull(mime);
    assertFalse(mime.isBlank());
  }

  protected final void assertMapsDataIfPresentIsObject(JsonObject data) {
    if (!data.has(MAPS_DATA_KEY) || data.get(MAPS_DATA_KEY).isJsonNull()) {
      return;
    }
    assertTrue(data.get(MAPS_DATA_KEY).isJsonObject(), "data." + MAPS_DATA_KEY + " must be an object");
  }

  protected final void assertHasNonBlankString(JsonObject obj, String key) {
    assertTrue(obj.has(key), "Missing '" + key + "'");
    JsonElement element = obj.get(key);
    assertNotNull(element);
    assertFalse(element.isJsonNull(), key + " is null");
    assertTrue(element.isJsonPrimitive(), key + " must be a primitive");
    String value = element.getAsString();
    assertNotNull(value);
    assertFalse(value.isBlank(), key + " is blank");
  }
}
