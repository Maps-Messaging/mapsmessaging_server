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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class N2kJsonDispatcherTest {

  @Test
  void registry_containsExpectedListenerForEachSupportedPgn() {
    N2kJsonListenerRegistry registry = new N2kJsonListenerRegistry();
    Map<Integer, Class<? extends N2kJsonListener>> expected = Map.ofEntries(
        Map.entry(N2kPgns.POSITION_RAPID_UPDATE, N2kPositionJsonListener.class),
        Map.entry(N2kPgns.GNSS_POSITION_DATA, N2kGnssJsonListener.class),
        Map.entry(N2kPgns.COG_SOG_RAPID_UPDATE, N2kMotionJsonListener.class),
        Map.entry(N2kPgns.VESSEL_HEADING, N2kHeadingJsonListener.class),
        Map.entry(N2kPgns.ATTITUDE, N2kAttitudeJsonListener.class),
        Map.entry(N2kPgns.RATE_OF_TURN, N2kRateOfTurnJsonListener.class),
        Map.entry(N2kPgns.GNSS_DOPS, N2kGnssDopsJsonListener.class),
        Map.entry(N2kPgns.BATTERY_STATUS, N2kBatteryStatusJsonListener.class),
        Map.entry(N2kPgns.MAGNETIC_VARIATION, N2kMagneticVariationJsonListener.class),
        Map.entry(N2kPgns.WIND_DATA, N2kWindJsonListener.class),
        Map.entry(N2kPgns.ENVIRONMENTAL_PARAMETERS, N2kEnvironmentalParametersJsonListener.class),
        Map.entry(N2kPgns.INVERTER_STATUS, N2kInverterStatusJsonListener.class));

    expected.forEach((pgn, listenerClass) -> {
      assertTrue(registry.hasListener(pgn));
      assertInstanceOf(listenerClass, registry.getListener(pgn));
    });
    assertFalse(registry.hasListener(999_999));
  }

  @Test
  void registry_duplicatePgn_throwsInsteadOfSilentlyReplacingListener() throws ReflectiveOperationException {
    N2kJsonListenerRegistry registry = new N2kJsonListenerRegistry();
    Method register = N2kJsonListenerRegistry.class.getDeclaredMethod("register", N2kJsonListener.class);
    register.setAccessible(true);
    RecordingListener duplicate = new RecordingListener(N2kPgns.VESSEL_HEADING);

    InvocationTargetException exception = assertThrows(
        InvocationTargetException.class,
        () -> register.invoke(registry, duplicate));

    IllegalArgumentException cause = assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    assertTrue(cause.getMessage().contains(String.valueOf(N2kPgns.VESSEL_HEADING)));
  }

  @Test
  void dispatch_supportedPgn_passesOriginalObjectsToListener() throws ReflectiveOperationException {
    RecordingListener listener = new RecordingListener(129_998);
    N2kJsonListenerRegistry registry = new N2kJsonListenerRegistry();
    listenerMap(registry).put(listener.getPgn(), listener);
    N2kJsonDispatcher dispatcher = new N2kJsonDispatcher(registry);
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.addProperty("value", 12);
    TwinUpdateContext context = context();

    dispatcher.dispatch(droneTwin, listener.getPgn(), packet, context);

    assertEquals(1, listener.invocations);
    assertSame(droneTwin, listener.droneTwin);
    assertSame(packet, listener.packet);
    assertSame(context, listener.context);
  }

  @Test
  void dispatch_ignoredUnknownPgn_doesNotWriteDiagnostic() {
    N2kJsonDispatcher dispatcher = new N2kJsonDispatcher(new N2kJsonListenerRegistry());
    ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
    PrintStream originalError = System.err;

    try {
      System.setErr(new PrintStream(errorBytes, true, StandardCharsets.UTF_8));
      dispatcher.dispatch(new DroneTwin(), N2kPgns.HEARTBEAT, new JsonObject(), context());
    } finally {
      System.setErr(originalError);
    }

    assertEquals("", errorBytes.toString(StandardCharsets.UTF_8));
  }

  @Test
  void dispatch_unhandledPgn_writesDiagnosticAndReturns() {
    N2kJsonDispatcher dispatcher = new N2kJsonDispatcher(new N2kJsonListenerRegistry());
    ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
    PrintStream originalError = System.err;

    try {
      System.setErr(new PrintStream(errorBytes, true, StandardCharsets.UTF_8));
      dispatcher.dispatch(new DroneTwin(), 129_999, new JsonObject(), context());
    } finally {
      System.setErr(originalError);
    }

    assertTrue(errorBytes.toString(StandardCharsets.UTF_8).contains("No listener for 129999"));
  }

  @Test
  void dispatch_malformedField_allowsListenerToUseOtherFields() {
    N2kJsonDispatcher dispatcher = new N2kJsonDispatcher(new N2kJsonListenerRegistry());
    DroneTwin droneTwin = new DroneTwin();
    JsonObject packet = new JsonObject();
    packet.add("headingSensorReading", new JsonObject());
    packet.addProperty("variation", Math.PI / 6.0d);

    assertDoesNotThrow(() -> dispatcher.dispatch(
        droneTwin,
        N2kPgns.VESSEL_HEADING,
        packet,
        context()));

    assertEquals("29.999999999999996", droneTwin.getAttributes().get("n2k.heading.variationDegrees"));
  }


  @SuppressWarnings("unchecked")
  private static Map<Integer, N2kJsonListener> listenerMap(N2kJsonListenerRegistry registry)
      throws ReflectiveOperationException {
    Field field = N2kJsonListenerRegistry.class.getDeclaredField("listeners");
    field.setAccessible(true);
    return (Map<Integer, N2kJsonListener>) field.get(registry);
  }

  private static TwinUpdateContext context() {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setReceivedTime(Instant.parse("2026-07-28T10:15:30Z"));
    return context;
  }

  private static final class RecordingListener implements N2kJsonListener {

    private final int pgn;
    private int invocations;
    private DroneTwin droneTwin;
    private JsonObject packet;
    private TwinUpdateContext context;

    private RecordingListener(int pgn) {
      this.pgn = pgn;
    }

    @Override
    public int getPgn() {
      return pgn;
    }

    @Override
    public void handle(DroneTwin droneTwin, JsonObject packet, TwinUpdateContext context) {
      invocations++;
      this.droneTwin = droneTwin;
      this.packet = packet;
      this.context = context;
    }
  }
}
