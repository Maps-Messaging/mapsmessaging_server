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

import com.google.gson.*;
import io.mapsmessaging.MessageDaemon;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.firstNonEmpty;

public abstract class PackHelper {
  protected static final String MAPS_DATA_KEY = "_mapsData";
  protected static final String PAYLOAD_KEY = "_payload";
  protected static final String PAYLOAD_BASE64_KEY = "payload_base64";
  protected static final String PAYLOAD_MIME_KEY = "payload_mime";

  protected final Gson gson;

  protected PackHelper(@NotNull Gson gson) {
    this.gson = gson;
  }

  public @NotNull JsonObject toCloudEventObject(
      @NotNull Message message,
      @NotNull String sourceUri
  ) {
    SchemaConfig schemaConfig = null;
    String schemaUri = "";
    if (message.getSchemaId() != null) {
      schemaConfig = SchemaManager.getInstance().getSchema(message.getSchemaId());
      if(schemaConfig != null) {
        schemaUri = MessageDaemon.getInstance().getRestServerUrl()+"api/v1/server/schema/impl/"+schemaConfig.getUniqueId();
      }
    }

    String subject = null;
    if (schemaConfig != null) {
      subject = firstNonEmpty(schemaConfig.getName(), schemaConfig.getTitle(), schemaConfig.getMatchExpression());
    }

    MessageFormatter formatter = null;
    if (schemaConfig != null) {
      try {
        formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
      } catch (Exception ignore) { }
    }

    String eventType = (schemaConfig != null)
        ? CloudEventTypeHelper.deriveEventType(schemaConfig)
        : "com.maps.raw.event";

    JsonObject cloudEvent = new JsonObject();
    packCloudEventAttributes(message, cloudEvent, sourceUri, eventType, subject);

    if (schemaConfig != null) {
      packSchemaExtensions(cloudEvent, schemaConfig);
    }

    packPayload(message, cloudEvent, formatter, schemaConfig, schemaUri);

    packAttributes(message, cloudEvent);
    packMetaData(message, cloudEvent);

    return cloudEvent;
  }

  public @NotNull byte[] toCloudEventBytes(
      @NotNull Message message,
      @NotNull String sourceUri
  ) {
    return gson.toJson(toCloudEventObject(message, sourceUri)).getBytes(StandardCharsets.UTF_8);
  }

  protected abstract void packPayload(
      @NotNull Message message,
      @NotNull JsonObject cloudEvent,
      @Nullable MessageFormatter formatter,
      @Nullable SchemaConfig schemaConfig,
      @Nullable String schemaUri
  );

  protected final void addDatacontenttypeIfAbsent(@NotNull JsonObject ce, @NotNull String mime) {
    if (!ce.has("datacontenttype")) {
      ce.addProperty("datacontenttype", mime);
    }
  }

  protected final void setDataschemaIfPresent(@NotNull JsonObject ce, @Nullable String schemaUri) {
    if (schemaUri != null && !schemaUri.isEmpty()) {
      ce.addProperty("dataschema", schemaUri);
    }
  }

  protected final @Nullable JsonObject buildMapsDataNode(@NotNull Message message) {
    if (message.getDataMap() == null || message.getDataMap().isEmpty()) {
      return null;
    }
    JsonObject node = new JsonObject();
    for (Map.Entry<String, TypedData> entry : message.getDataMap().entrySet()) {
      Object value = entry.getValue() != null ? entry.getValue().getData() : null;
      node.add(entry.getKey(), gson.toJsonTree(value));
    }
    return node;
  }

  private void packCloudEventAttributes(
      @NotNull Message message,
      @NotNull JsonObject cloudEvent,
      @NotNull String sourceUri,
      @NotNull String eventType,
      @Nullable String subject
  ) {
    long identifier = message.getIdentifier();
    String eventId = (identifier > 0) ? Long.toString(identifier) : UUID.randomUUID().toString();

    cloudEvent.addProperty("specversion", "1.0");
    cloudEvent.addProperty("id", eventId);
    cloudEvent.addProperty("source", sourceUri);
    cloudEvent.addProperty("type", eventType);

    if (subject != null && !subject.isEmpty()) {
      cloudEvent.addProperty("subject", subject);
    }
  }

  private void packSchemaExtensions(@NotNull JsonObject ce, @NotNull SchemaConfig schema) {
    if (schema.getUniqueId() != null && !schema.getUniqueId().isEmpty()) {
      ce.addProperty("mapsSchemaId", schema.getUniqueId());
    }
    if (schema.getFormat() != null && !schema.getFormat().isEmpty()) {
      ce.addProperty("mapsSchemaFormat", schema.getFormat());
    }
    if (schema.getVersion() > 0) {
      ce.addProperty("mapsSchemaVersion", Integer.toString(schema.getVersion()));
    }
    if (schema.getTitle() != null && !schema.getTitle().isEmpty()) {
      ce.addProperty("mapsSchemaTitle", schema.getTitle());
    }
  }

  private void packAttributes(@NotNull Message message, @NotNull JsonObject cloudEvent) {
    long creationMillis = message.getCreation();
    if (creationMillis > 0) {
      cloudEvent.addProperty("time", Instant.ofEpochMilli(creationMillis).toString());
    }

    Priority priority = message.getPriority();
    if (priority != null) {
      cloudEvent.addProperty("mapsPriority", priority.name());
    }

    QualityOfService qualityOfService = message.getQualityOfService();
    if (qualityOfService != null) {
      cloudEvent.addProperty("mapsQoS", qualityOfService.name());
    }

    cloudEvent.addProperty("mapsRetain", message.isRetain());
    cloudEvent.addProperty("mapsStoreOffline", message.isStoreOffline());

    if (message.getResponseTopic() != null && !message.getResponseTopic().isEmpty()) {
      cloudEvent.addProperty("mapsResponseTopic", message.getResponseTopic());
    }

    Object correlation = message.getCorrelationData();
    if (correlation instanceof byte[] bytes) {
      cloudEvent.addProperty("mapsCorrelationBase64", Base64.getEncoder().encodeToString(bytes));
    } else if (correlation instanceof String s && !s.isEmpty()) {
      cloudEvent.addProperty("mapsCorrelation", s);
    }
  }

  private void packMetaData(@NotNull Message message, @NotNull JsonObject cloudEvent) {
    if (message.getMeta() == null || message.getMeta().isEmpty()) {
      return;
    }
    for (Map.Entry<String, String> metaEntry : message.getMeta().entrySet()) {
      String originalKey = metaEntry.getKey();
      String key = "mapsMeta_" + sanitizeExtensionKey(originalKey);
      String value = metaEntry.getValue();

      if ("route".equals(originalKey)) {
        try {
          JsonElement parsed = JsonParser.parseString(value);
          if (parsed.isJsonArray()) {
            cloudEvent.add(key, parsed.getAsJsonArray());
            continue;
          }
        } catch (Exception ignore) { }
      }
      cloudEvent.addProperty(key, value);
    }
  }

  protected final @NotNull String sanitizeExtensionKey(@NotNull String key) {
    return key.replaceAll("[^A-Za-z0-9_]", "_");
  }

  protected static String resolveMimeType(@NotNull Message message, @Nullable SchemaConfig schemaConfig) {
    String contentType = message.getContentType();
    if (contentType != null && !contentType.isEmpty()) return contentType;

    if (schemaConfig != null) {
      String schemaMimeType = schemaConfig.getMimeType();
      if (schemaMimeType != null && !schemaMimeType.isEmpty()) return schemaMimeType;

      String format = schemaConfig.getFormat();
      if (format != null) {
        String f = format.toLowerCase(java.util.Locale.ROOT);
        if (f.equals("protobuf")) return "application/x-protobuf";
        if (f.equals("avro")) return "avro/binary";            // or "application/avro"
        if (f.equals("cbor")) return "application/cbor";
        if (f.equals("messagepack") || f.equals("msgpack")) return "application/msgpack";
        if (f.equals("json")) return "application/json";
        if (f.equals("csv")) return "text/csv";
        if (f.equals("xml")) return "application/xml";
      }
    }
    return "application/octet-stream";
  }
}
