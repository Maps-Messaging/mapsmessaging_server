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

package io.mapsmessaging.rest.translation;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonHandlerTest {

  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  @Test
  void instant_adapter_round_trips_precise_value() throws Exception {
    InstantTypeAdapter adapter = new InstantTypeAdapter();
    Instant expected = Instant.parse("2026-06-14T03:04:05.123456789Z");
    StringWriter output = new StringWriter();

    adapter.write(new JsonWriter(output), expected);

    String expectedString ="2026-06-14T03:04:05.123Z";
    assertEquals("\"2026-06-14T03:04:05.123+00:00\"", output.toString());
    assertEquals(expectedString, adapter.read(new JsonReader(new StringReader(output.toString()))).toString());
  }

  @Test
  void instant_adapter_maps_null_and_blank_values_to_null() throws Exception {
    InstantTypeAdapter adapter = new InstantTypeAdapter();
    StringWriter output = new StringWriter();

    adapter.write(new JsonWriter(output), null);

    assertEquals("null", output.toString());
    assertNull(adapter.read(new JsonReader(new StringReader("null"))));
    assertNull(adapter.read(new JsonReader(new StringReader("\"  \""))));
  }

  @Test
  void instant_adapter_rejects_malformed_value() {
    InstantTypeAdapter adapter = new InstantTypeAdapter();
    JsonReader reader = new JsonReader(new StringReader("\"not-an-instant\""));

    assertThrows(DateTimeException.class, () -> adapter.read(reader));
  }

  @Test
  void handler_round_trips_utf8_and_registered_time_type() throws Exception {
    JsonHandler handler = new JsonHandler();
    SamplePayload expected = new SamplePayload("München 東京", LocalDateTime.of(2026, 6, 14, 13, 45, 12));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    handler.writeTo(expected, SamplePayload.class, SamplePayload.class, NO_ANNOTATIONS, null, null, output);
    SamplePayload actual = (SamplePayload) handler.readFrom(
        castClass(SamplePayload.class),
        SamplePayload.class,
        NO_ANNOTATIONS,
        null,
        null,
        new ByteArrayInputStream(output.toByteArray())
    );

    assertEquals(expected.name(), actual.name());
    assertEquals(expected.createdAt(), actual.createdAt());
    assertTrue(output.toString(StandardCharsets.UTF_8).contains("München 東京"));
  }

  @Test
  void handler_honours_generic_type_when_reading_lists() throws Exception {
    JsonHandler handler = new JsonHandler();
    Type listType = new TypeToken<List<SamplePayload>>() { }.getType();
    String json = """
        [
          {
            "name": "sensor",
            "createdAt": "2026-06-14 13:45:12"
          }
        ]
        """;

    Object result = handler.readFrom(
        castClass(List.class),
        listType,
        NO_ANNOTATIONS,
        null,
        null,
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
    );

    List<?> payloads = assertInstanceOf(List.class, result);
    SamplePayload payload = assertInstanceOf(SamplePayload.class, payloads.get(0));
    assertEquals("sensor", payload.name());
    assertEquals(LocalDateTime.of(2026, 6, 14, 13, 45, 12), payload.createdAt());
  }

  @SuppressWarnings("unchecked")
  private static Class<Object> castClass(Class<?> type) {
    return (Class<Object>) type;
  }

  private record SamplePayload(String name, LocalDateTime createdAt) {
  }
}
