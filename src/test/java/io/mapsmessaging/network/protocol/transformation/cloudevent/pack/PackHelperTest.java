package io.mapsmessaging.network.protocol.transformation.cloudevent.pack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackHelperTest {

  @Test
  void rawMessageProducesCoreCloudEventAndMapsExtensions() {
    Message message = mock(Message.class);
    when(message.getSchemaId()).thenReturn(null);
    when(message.getIdentifier()).thenReturn(42L);
    when(message.getOpaqueData()).thenReturn("payload".getBytes(StandardCharsets.UTF_8));
    when(message.getDataMap()).thenReturn(Map.of("count", new TypedData(3)));
    when(message.getMeta()).thenReturn(Map.of(
        "route", "[\"a\",\"b\"]",
        "edge-name", "edge-1"));
    when(message.getCorrelationData()).thenReturn(new byte[]{1,2,3});
    when(message.getResponseTopic()).thenReturn("/reply");

    TestPackHelper helper = new TestPackHelper();
    JsonObject event = helper.toCloudEventObject(message, "urn:maps:test");

    assertEquals("1.0", event.get("specversion").getAsString());
    assertEquals("42", event.get("id").getAsString());
    assertEquals("urn:maps:test", event.get("source").getAsString());
    assertEquals("com.maps.raw.event", event.get("type").getAsString());
    assertEquals("payload", event.get("data").getAsString());
    assertTrue(event.get("mapsMeta_route").isJsonArray());
    assertEquals("edge-1", event.get("mapsMeta_edge_name").getAsString());
    assertEquals("/reply", event.get("mapsResponseTopic").getAsString());
    assertNotNull(helper.mapsData(message));
  }

  @Test
  void mimeResolutionPrefersMessageContentTypeThenSchemaFormat() {
    Message message = mock(Message.class);
    SchemaConfig schema = mock(SchemaConfig.class);
    when(message.getContentType()).thenReturn("application/custom");

    assertEquals(
        "application/custom",
        TestPackHelper.mime(message, schema));

    when(message.getContentType()).thenReturn(null);
    when(schema.getMimeType()).thenReturn(null);
    when(schema.getFormat()).thenReturn("json");

    assertEquals("application/json", TestPackHelper.mime(message, schema));
  }

  private static final class TestPackHelper extends PackHelper {
    TestPackHelper() {
      super(new Gson());
    }

    @Override
    protected void packPayload(
        Message message,
        JsonObject cloudEvent,
        MessageFormatter formatter,
        SchemaConfig schemaConfig,
        String schemaUri) {
      cloudEvent.addProperty(
          "data",
          new String(message.getOpaqueData(), StandardCharsets.UTF_8));
    }

    JsonObject mapsData(Message message) {
      return buildMapsDataNode(message);
    }

    static String mime(Message message, SchemaConfig schema) {
      return resolveMimeType(message, schema);
    }
  }
}
