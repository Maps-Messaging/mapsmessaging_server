package io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class ElementTypeAdapterTest {

  @Test
  void everyElementTypeRoundTripsByWireAttribute() throws Exception {
    ElementTypeAdapter adapter = new ElementTypeAdapter();

    for (ElementType type : ElementType.values()) {
      StringWriter buffer = new StringWriter();
      JsonWriter writer = new JsonWriter(buffer);
      adapter.write(writer, type);
      writer.flush();

      assertEquals('"' + type.getAttribute() + '"', buffer.toString());

      JsonReader reader = new JsonReader(new StringReader(buffer.toString()));
      assertSame(type, adapter.read(reader));
    }
  }

  @Test
  void readsAttributesCaseInsensitively() throws Exception {
    ElementTypeAdapter adapter = new ElementTypeAdapter();

    JsonReader reader = new JsonReader(new StringReader(""BoOlEaN""));

    assertSame(ElementType.BOOLEAN, adapter.read(reader));
  }

  @Test
  void unknownAttributeIsRejected() {
    ElementTypeAdapter adapter = new ElementTypeAdapter();

    assertThrows(
        IllegalArgumentException.class,
        () -> adapter.read(new JsonReader(new StringReader(""mystery"")))
    );
  }

  @Test
  void nullShouldRoundTripSymmetrically() throws Exception {
    ElementTypeAdapter adapter = new ElementTypeAdapter();
    StringWriter buffer = new StringWriter();
    JsonWriter writer = new JsonWriter(buffer);

    adapter.write(writer, null);
    writer.flush();
    assertEquals("null", buffer.toString());

    assertNull(
        adapter.read(new JsonReader(new StringReader("null"))),
        "Adapter writes null and should therefore also be able to read null"
    );
  }
}
