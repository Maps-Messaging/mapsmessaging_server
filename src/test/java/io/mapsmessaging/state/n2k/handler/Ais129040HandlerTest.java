package io.mapsmessaging.state.n2k.handler;

import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.DroneEmissionState;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Ais129040HandlerTest {

  @Test
  void firstEligibleExtendedPositionEmits() {
    Ais129040Handler handler =
        new Ais129040Handler(
            parserReturning(new byte[]{4, 5}),
            AisClassBEmitterConfig.getDefaults(),
            1000);

    PgnEmission emission =
        handler.emit(Ais129039HandlerTest.eligibleTwin(), new DroneEmissionState(), 1000)
            .orElseThrow();

    assertEquals(129040, emission.getPgn());
    assertArrayEquals(new byte[]{4, 5}, emission.getPayload());
  }

  @Test
  void unchangedTwinIsSuppressedButMaterialChangeEmitsBeforeInterval() {
    Ais129040Handler handler =
        new Ais129040Handler(
            parserReturning(new byte[]{1}),
            AisClassBEmitterConfig.getDefaults(),
            1000);
    DroneTwin twin = Ais129039HandlerTest.eligibleTwin();
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    assertTrue(handler.emit(twin, state, 1100).isEmpty());

    twin.setHeadingDegrees(20.0);
    assertTrue(handler.emit(twin, state, 1200).isPresent());
  }

  @Test
  void intervalExpiryEmitsEvenWithoutMotionChange() {
    Ais129040Handler handler =
        new Ais129040Handler(
            parserReturning(new byte[]{1}),
            AisClassBEmitterConfig.getDefaults(),
            1000);
    DroneTwin twin = Ais129039HandlerTest.eligibleTwin();
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    assertTrue(handler.emit(twin, state, 2000).isPresent());
  }

  @Test
  void invalidTwinAndEmptyPayloadAreSuppressed() {
    N2kMessageParser parser = parserReturning(new byte[0]);
    Ais129040Handler handler =
        new Ais129040Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000);

    assertTrue(handler.emit(null, new DroneEmissionState(), 0).isEmpty());
    verifyNoInteractions(parser);

    assertTrue(
        handler.emit(Ais129039HandlerTest.eligibleTwin(), new DroneEmissionState(), 0)
            .isEmpty());
  }

  private static N2kMessageParser parserReturning(byte[] payload) {
    N2kMessageParser parser = mock(N2kMessageParser.class);
    when(parser.encodeFromSource(eq(129040), any(FieldValueSource.class)))
        .thenReturn(payload);
    return parser;
  }
}
