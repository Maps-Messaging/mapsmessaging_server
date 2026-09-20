package io.mapsmessaging.network.protocol.transformation.cloudevent.pack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackHelperBranchCoverageTest {

  @Test
  void contentTypeIsOnlyAddedWhenAbsentAndSchemaNeedsNonBlankUri() {
    TestHelper helper = new TestHelper();
    JsonObject event = new JsonObject();

    helper.addMime(event, "application/json");
    helper.addMime(event, "application/xml");
    assertEquals("application/json", event.get("datacontenttype").getAsString());

    helper.addSchema(event, null);
    helper.addSchema(event, "");
    assertFalse(event.has("dataschema"));

    helper.addSchema(event, "urn:test:schema");
    assertEquals("urn:test:schema", event.get("dataschema").getAsString());
  }

  @Test
  void mapsDataReturnsNullForMissingOrEmptyMapAndPreservesNullTypedEntries() {
    TestHelper helper = new TestHelper();
    Message message = mock(Message.class);

    when(message.getDataMap()).thenReturn(null);
    assertNull(helper.mapsData(message));

    when(message.getDataMap()).thenReturn(Map.of());
    assertNull(helper.mapsData(message));

    Map<String, TypedData> values = new LinkedHashMap<>();
    values.put("present", new TypedData("value"));
    values.put("nullEntry", null);
    when(message.getDataMap()).thenReturn(values);
    JsonObject node = helper.mapsData(message);
    assertEquals("value", node.get("present").getAsString());
    assertTrue(node.get("nullEntry").isJsonNull());
  }

  private static final class TestHelper extends PackHelper {
    TestHelper() {
      super(new Gson());
    }

    @Override
    protected void packPayload(
        Message message,
        JsonObject cloudEvent,
        MessageFormatter formatter,
        SchemaConfig schemaConfig,
        String schemaUri) {
    }

    void addMime(JsonObject event, String mime) {
      addDatacontenttypeIfAbsent(event, mime);
    }

    void addSchema(JsonObject event, String uri) {
      setDataschemaIfPresent(event, uri);
    }

    JsonObject mapsData(Message message) {
      return buildMapsDataNode(message);
    }
  }
}