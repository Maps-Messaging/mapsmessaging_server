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
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

public final class EnvelopePackHelper extends PackHelper {

  public EnvelopePackHelper(com.google.gson.Gson gson) {
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
    JsonObject envelope = new JsonObject();

    byte[] bytes = message.getOpaqueData();
    if (bytes != null && bytes.length > 0) {
      String mime = message.getContentType();
      if (mime == null || mime.isEmpty()) {
        mime = resolveMimeType(message, schemaConfig);
      }
      envelope.addProperty(PAYLOAD_MIME_KEY, mime);
      if(schemaConfig instanceof JsonSchemaConfig){
        JsonElement parsed = JsonParser.parseString(new String(bytes));
        envelope.add(PAYLOAD_KEY, parsed);
      }
      else {
        envelope.addProperty(PAYLOAD_BASE64_KEY, Base64.getEncoder().encodeToString(bytes));
      }
    }

    JsonObject mapsData = buildMapsDataNode(message);
    if (mapsData != null) {
      envelope.add(MAPS_DATA_KEY, mapsData);
    }

    if (!envelope.isEmpty()) {
      cloudEvent.add("data", envelope);
      addDatacontenttypeIfAbsent(cloudEvent, "application/json");
      setDataschemaIfPresent(cloudEvent, schemaUri);
    }
  }
}
