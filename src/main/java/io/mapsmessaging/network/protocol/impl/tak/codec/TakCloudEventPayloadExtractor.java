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

package io.mapsmessaging.network.protocol.impl.tak.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class TakCloudEventPayloadExtractor {

  private TakCloudEventPayloadExtractor() {
  }

  static byte[] tryExtractPayload(byte[] opaqueData) {
    if (opaqueData == null || opaqueData.length == 0) {
      return null;
    }
    String jsonCandidate = new String(opaqueData, StandardCharsets.UTF_8).trim();
    if (!jsonCandidate.startsWith("{")) {
      return null;
    }
    try {
      JsonElement rootElement = JsonParser.parseString(jsonCandidate);
      if (!rootElement.isJsonObject()) {
        return null;
      }
      JsonObject root = rootElement.getAsJsonObject();
      if (!root.has("specversion")) {
        return null;
      }

      JsonElement base64Element = root.get("data_base64");
      if (base64Element != null && base64Element.isJsonPrimitive()) {
        return Base64.getDecoder().decode(base64Element.getAsString());
      }

      JsonElement dataElement = root.get("data");
      if (dataElement == null || !dataElement.isJsonObject()) {
        return null;
      }
      JsonObject data = dataElement.getAsJsonObject();
      JsonElement payloadBase64 = data.get("payload_base64");
      if (payloadBase64 != null && payloadBase64.isJsonPrimitive()) {
        return Base64.getDecoder().decode(payloadBase64.getAsString());
      }
      return null;
    } catch (Exception ignored) {
      return null;
    }
  }
}

