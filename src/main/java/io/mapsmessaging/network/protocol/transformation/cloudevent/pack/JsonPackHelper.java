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

package io.mapsmessaging.network.protocol.transformation.cloudevent.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JsonPackHelper extends PackHelper {

  public JsonPackHelper(com.google.gson.Gson gson) {
    super(gson);
  }

  @Override
  protected void packPayload(
      Message message,
      JsonObject cloudEvent,
      @Nullable MessageFormatter formatter,
      @Nullable SchemaConfig schemaConfig,
      @Nullable String schemaUri
  ) {
    JsonObject data = null;

    byte[] bytes = message.getOpaqueData();
    if (bytes != null && bytes.length > 0) {
      if (formatter != null) {
        try {
          JsonObject normalized = formatter.parseToJson(bytes);
          if (normalized != null) {
            data = normalized;
          }
        } catch (Exception ignore) {
          // if we can not parse to json, we fall through and base64 it
        }
      }

      if (data == null && message.isUTF8()) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        try {
          JsonElement parsed = JsonParser.parseString(utf8);
          if (parsed.isJsonObject()) {
            data = parsed.getAsJsonObject();
          } else {
            JsonObject wrapped = new JsonObject();
            wrapped.add(PAYLOAD_KEY, parsed);
            data = wrapped;
          }
        } catch (Exception ignore) {
          // if we can not parse to json, we fall through and base64 it
        }
      }

      if (data == null) {
        JsonObject wrapped = new JsonObject();
        wrapped.addProperty(PAYLOAD_BASE64_KEY, Base64.getEncoder().encodeToString(bytes));
        String mime = message.getContentType() != null ? message.getContentType() : "application/octet-stream";
        wrapped.addProperty(PAYLOAD_MIME_KEY, mime);
        data = wrapped;
      }
    }

    JsonObject mapsData = buildMapsDataNode(message);
    if (mapsData != null) {
      if (data == null) {
        data = new JsonObject();
      }
      data.add(MAPS_DATA_KEY, mapsData);
    }

    if (data != null) {
      cloudEvent.add("data", data);
      addDatacontenttypeIfAbsent(cloudEvent, "application/json");
      setDataschemaIfPresent(cloudEvent, schemaUri);
    }
  }
}
