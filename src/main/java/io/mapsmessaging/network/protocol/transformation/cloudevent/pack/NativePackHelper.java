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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class NativePackHelper extends PackHelper {

  public NativePackHelper(com.google.gson.Gson gson) {
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
    byte[] bytes = message.getOpaqueData();
    if (bytes == null || bytes.length == 0) return;

    // Resolve MIME once
    String mime = resolveMimeType(message, schemaConfig);

    boolean emittedJson = false;

    if (schemaConfig instanceof JsonSchemaConfig) {
      // Try to parse JSON using charset (default UTF-8); fallback to base64 on failure
      Charset cs = charsetFromContentType(mime);
      try {
        String text = new String(bytes, cs);
        JsonElement parsed = JsonParser.parseString(text);
        cloudEvent.add("data", parsed);
        cloudEvent.addProperty("datacontenttype", "application/json"); // force correct CE MIME
        emittedJson = true;
      } catch (Exception ignore) {
        // fall through to binary branch below
      }
    }

    if (!emittedJson) {
      cloudEvent.addProperty("data_base64", Base64.getEncoder().encodeToString(bytes));
      cloudEvent.addProperty("datacontenttype", mime); // native MIME for binary branch
    }

    setDataschemaIfPresent(cloudEvent, schemaUri);
  }

  private static Charset charsetFromContentType(String contentType) {
    if (contentType == null) return StandardCharsets.UTF_8;
    int i = contentType.toLowerCase().indexOf("charset=");
    if (i >= 0) {
      String v = contentType.substring(i + 8).trim();
      int sc = v.indexOf(';');
      if (sc >= 0) v = v.substring(0, sc).trim();
      try {
        return Charset.forName(v.replace("\"", "").trim());
      } catch (Exception ignore) { }
    }
    return StandardCharsets.UTF_8;
  }
}
