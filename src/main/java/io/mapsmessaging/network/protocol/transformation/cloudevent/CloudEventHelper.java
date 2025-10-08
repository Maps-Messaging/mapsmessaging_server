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

package io.mapsmessaging.network.protocol.transformation.cloudevent;

import com.google.gson.*;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;


import static org.apache.commons.lang3.StringUtils.firstNonEmpty;

public final class CloudEventHelper {
  private static final String MAPS_DATA_KEY = "_mapsData";
  private static final String PAYLOAD_KEY = "_payload";
  private static final String PAYLOAD_BASE64_KEY = "payload_base64";
  private static final String PAYLOAD_MIME_KEY = "payload_mime";


  private CloudEventHelper() { }

  /**
   * Build a CloudEvents 1.0 JSON object from a MessageBuilder.
   *
   * @param message source message
   * @param sourceUri required CloudEvents "source" (URI or URI-reference)
   * @param schemaUri optional schema locator (e.g., maps://server-id/$SCHEMA/<fqn>)
   * @param gson Gson instance to use
   * @return JsonObject representing a CloudEvent
   */
  public static @NotNull byte[] toCloudEvent(
      @NotNull Message message,
      @NotNull String sourceUri,
      @Nullable String schemaUri,
      @NotNull Gson gson
  ) {
    SchemaConfig schemaConfig = SchemaManager.getInstance().getSchema(message.getSchemaId());

    String subject = firstNonEmpty(
        schemaConfig.getName(),
        schemaConfig.getTitle(),
        schemaConfig.getMatchExpression()
    );

    MessageFormatter formatter = null;
    try{
      formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    }
    catch(Exception e){
      // log this
    }
    String eventType = CloudEventTypeHelper.deriveEventType(schemaConfig);
    JsonObject cloudEvent = new JsonObject();
    packCloudEventAttributes(message, cloudEvent, sourceUri, eventType, subject);
    packSchemaDetails(message, cloudEvent, schemaConfig, schemaUri);

    JsonObject payloadObj = packData(message, formatter, gson);
    JsonObject mapObj = packDataMap(message, gson);
    JsonObject combined = combineData(payloadObj, mapObj);

    if (combined != null && !combined.isEmpty()) {
      cloudEvent.add("data", combined);
      if (!cloudEvent.has("datacontenttype")) {
        cloudEvent.addProperty("datacontenttype", "application/json");
      }
    }
    packAttributes(message, cloudEvent);
    packMetaData(message, cloudEvent);

    return gson.toJson(cloudEvent).getBytes();
  }

  private static void packCloudEventAttributes(Message message, @NotNull JsonObject cloudEvent,  String sourceUri,String eventType,String subject) {
    String eventId = ""+message.getIdentifier();
    cloudEvent.addProperty("specversion", "1.0");
    cloudEvent.addProperty("id", eventId);
    cloudEvent.addProperty("source", sourceUri);
    cloudEvent.addProperty("type", eventType);

    if (subject != null && !subject.isEmpty()) {
      cloudEvent.addProperty("subject", subject);
    }
  }

  private static void packSchemaDetails(@NotNull Message message, @NotNull JsonObject ce, SchemaConfig schema, String schemaUri) {
    // --- Schema mapping (private registry friendly) ---
    if (schema.getUniqueId() != null && !schema.getUniqueId().isEmpty()) {
      ce.addProperty("mapsSchemaId", schema.getUniqueId());
    }
    ce.addProperty("mapsSchemaFormat", schema.getFormat());
    if (schema.getVersion() > 0) ce.addProperty("mapsSchemaVersion", Integer.toString(schema.getVersion()));
    if (schema.getTitle() != null && !schema.getTitle().isEmpty()) ce.addProperty("mapsSchemaTitle", schema.getTitle());
    if (schemaUri != null && !schemaUri.isEmpty()) {
      ce.addProperty("dataschema", schemaUri);
      ce.addProperty("mapsSchemaUri", schemaUri);
    }

    // Content type preference: schema.mimeType -> message.contentType
    String contentType = firstNonEmpty(schema.getMimeType(), message.getContentType());
    if (contentType != null && !contentType.isEmpty()) ce.addProperty("datacontenttype", contentType);
  }

  private static @Nullable JsonObject packData(
      @NotNull Message message,
      @Nullable MessageFormatter formatter,
      @NotNull Gson gson
  ) {
    byte[] opaque = message.getOpaqueData();
    if (opaque == null || opaque.length == 0) return null;

    // 1) Schema-normalized JSON (preferred when formatter is present)
    if (formatter != null) {
      try {
        JsonObject normalized = formatter.parseToJson(opaque);
        if (normalized != null) return normalized;
      } catch (IOException ignore) { }
    }

    // 2) UTF-8 probe → if JSON, return it as object (or wrap non-object)
    String ct = message.getContentType();
    if (message.isUTF8()) {
      String utf8 = new String(opaque, StandardCharsets.UTF_8);
      try {
        JsonElement parsed = JsonParser.parseString(utf8);
        if (parsed.isJsonObject()) return parsed.getAsJsonObject();
        JsonObject wrapped = new JsonObject();
        wrapped.add(PAYLOAD_KEY, parsed);
        if (ct != null && !ct.isEmpty()) wrapped.addProperty(PAYLOAD_MIME_KEY, ct);
        return wrapped;
      } catch (Exception ignore) {
        // fall through to base64 wrapper
      }
    }

    // 3) Binary → wrap as base64 inside JSON (so we can still merge maps)
    JsonObject wrapped = new JsonObject();
    wrapped.addProperty(PAYLOAD_BASE64_KEY, Base64.getEncoder().encodeToString(opaque));
    if (ct == null || ct.isEmpty()) ct = "application/octet-stream";
    wrapped.addProperty(PAYLOAD_MIME_KEY, ct);
    return wrapped;
  }

  /** Returns a JSON object containing the data map under flat keys. If none, returns null. */
  private static @Nullable JsonObject packDataMap(@NotNull Message message, @NotNull Gson gson) {
    if (message.getDataMap() == null || message.getDataMap().isEmpty()) return null;
    JsonObject obj = new JsonObject();
    for (Map.Entry<String, TypedData> e : message.getDataMap().entrySet()) {
      Object val = e.getValue() != null ? e.getValue().getData() : null;
      obj.add(e.getKey(), gson.toJsonTree(val));
    }
    return obj;
  }

  /** Combines payload JSON and data-map JSON into a single CE "data" object. */
  private static @Nullable JsonObject combineData(@Nullable JsonObject payload, @Nullable JsonObject dataMap) {
    if ((payload == null || payload.isEmpty()) && (dataMap == null || dataMap.isEmpty())) return null;
    if (payload == null || payload.isEmpty()) return dataMap;
    if (dataMap == null || dataMap.isEmpty()) return payload;

    JsonObject out = shallowCopy(payload);
    JsonObject mapsNode;
    if (out.has(MAPS_DATA_KEY) && out.get(MAPS_DATA_KEY).isJsonObject()) {
      mapsNode = out.getAsJsonObject(MAPS_DATA_KEY);
    } else {
      mapsNode = new JsonObject();
      out.add(MAPS_DATA_KEY, mapsNode);
    }
    mergeInto(mapsNode, dataMap);
    return out;
  }

  private static JsonObject shallowCopy(JsonObject src) {
    JsonObject dst = new JsonObject();
    for (Map.Entry<String, JsonElement> e : src.entrySet()) dst.add(e.getKey(), e.getValue());
    return dst;
  }

  private static void mergeInto(JsonObject target, JsonObject src) {
    for (Map.Entry<String, JsonElement> e : src.entrySet()) target.add(e.getKey(), e.getValue());
  }

  private static void packAttributes(Message messageBuilder, JsonObject cloudEvent) {
    long creationMillis = messageBuilder.getCreation();
    if (creationMillis > 0) {
      cloudEvent.addProperty("time", Instant.ofEpochMilli(creationMillis).toString());
    }

    Priority priority = messageBuilder.getPriority();
    if (priority != null) {
      cloudEvent.addProperty("mapsPriority", priority.name());
    }

    QualityOfService qualityOfService = messageBuilder.getQualityOfService();
    if (qualityOfService != null) {
      cloudEvent.addProperty("mapsQoS", qualityOfService.name());
    }

    cloudEvent.addProperty("mapsRetain", messageBuilder.isRetain());
    cloudEvent.addProperty("mapsStoreOffline", messageBuilder.isStoreOffline());

    if (messageBuilder.getResponseTopic() != null && !messageBuilder.getResponseTopic().isEmpty()) {
      cloudEvent.addProperty("mapsResponseTopic", messageBuilder.getResponseTopic());
    }

    Object correlation = messageBuilder.getCorrelationData();
    if (correlation instanceof byte[] bytes) {
      cloudEvent.addProperty("mapsCorrelationBase64", Base64.getEncoder().encodeToString(bytes));
    } else if (correlation instanceof String s && !s.isEmpty()) {
      cloudEvent.addProperty("mapsCorrelation", s);
    }
  }

  private static void packMetaData(Message messageBuilder, JsonObject cloudEvent) {
    if (messageBuilder.getMeta() != null && !messageBuilder.getMeta().isEmpty()) {
      for (Map.Entry<String, String> metaEntry : messageBuilder.getMeta().entrySet()) {
        String key = "mapsMeta_" + sanitizeExtensionKey(metaEntry.getKey());
        if(metaEntry.getKey().equals("route")){
          String json = metaEntry.getValue();
          JsonArray array = JsonParser.parseString(json).getAsJsonArray();
          cloudEvent.add(key, array);
        }
        else {
          cloudEvent.addProperty(key, metaEntry.getValue());
        }
      }
    }
  }

  private static @NotNull String sanitizeExtensionKey(@NotNull String key) {
    return key.replaceAll("[^A-Za-z0-9_]", "_");
  }
}
