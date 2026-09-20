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

package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AbstractN2kJsonListenerTest {

  private final TestListener listener = new TestListener();

  @Test
  void lookupHelpersUseFirstAvailableAliasAndHandleNullPackets() {
    JsonObject packet = new JsonObject();
    packet.addProperty("second", 12.5);
    packet.addProperty("integer", 7);
    packet.addProperty("long", 1234567890123L);
    packet.addProperty("text", "value");

    assertEquals(12.5, listener.doubleValue(packet, "first", "second"), 0.0);
    assertEquals(7, listener.integerValue(packet, "integer"));
    assertEquals(1234567890123L, listener.longValue(packet, "long"));
    assertEquals("value", listener.stringValue(packet, "text"));
    assertTrue(listener.any(packet, "missing", "second"));

    assertNull(listener.doubleValue(null, "x"));
    assertNull(listener.integerValue(new JsonObject(), "x"));
    assertFalse(listener.any(null, "x"));
  }

  @Test
  void coordinateValidationIncludesLegalBoundaries() {
    assertTrue(listener.validLatitude(-90.0));
    assertTrue(listener.validLatitude(90.0));
    assertFalse(listener.validLatitude(-90.1));
    assertFalse(listener.validLatitude(90.1));
    assertFalse(listener.validLatitude(null));

    assertTrue(listener.validLongitude(-180.0));
    assertTrue(listener.validLongitude(180.0));
    assertFalse(listener.validLongitude(-180.1));
    assertFalse(listener.validLongitude(180.1));
    assertFalse(listener.validLongitude(null));
  }

  @Test
  void angleHelpersHandleNullAndWrapAround() {
    assertNull(listener.normalise(null));
    assertEquals(350.0, listener.normalise(-10.0), 0.0);
    assertEquals(10.0, listener.normalise(370.0), 0.0);

    assertNull(listener.degrees(null));
    assertEquals(180.0, listener.degrees(Math.PI), 0.000001);
  }

  @Test
  void timestampUsesContextWhenAvailable() {
    TwinUpdateContext context = new TwinUpdateContext();
    Instant expected = Instant.parse("2026-09-19T20:20:00Z");
    context.setReceivedTime(expected);

    assertEquals(expected, listener.timestamp(context));
    assertNotNull(listener.timestamp(null));
  }

  private static final class TestListener extends AbstractN2kJsonListener {
    @Override
    public int getPgn() {
      return 0;
    }

    @Override
    public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
    }

    Double doubleValue(JsonObject packet, String... names) {
      return getDouble(packet, names);
    }

    Integer integerValue(JsonObject packet, String... names) {
      return getInteger(packet, names);
    }

    Long longValue(JsonObject packet, String... names) {
      return getLong(packet, names);
    }

    String stringValue(JsonObject packet, String... names) {
      return getString(packet, names);
    }

    boolean any(JsonObject packet, String... names) {
      return hasAny(packet, names);
    }

    boolean validLatitude(Double value) {
      return isValidLatitude(value);
    }

    boolean validLongitude(Double value) {
      return isValidLongitude(value);
    }

    Double normalise(Double value) {
      return normalizeDegrees(value);
    }

    Double degrees(Double value) {
      return radiansToDegrees(value);
    }

    Instant timestamp(TwinUpdateContext context) {
      return resolveTimestamp(context);
    }
  }
}
