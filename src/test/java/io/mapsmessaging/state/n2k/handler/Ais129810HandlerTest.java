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

class Ais129810HandlerTest {

  @Test
  void staticPartBEmitsThenSuppressesUnchangedSignatureUntilInterval() {
    Ais129810Handler handler =
        new Ais129810Handler(
            parserReturning(new byte[]{8}),
            AisClassBEmitterConfig.getDefaults(),
            1000);
    DroneTwin twin = twin("CALL1");
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    assertTrue(handler.emit(twin, state, 1200).isEmpty());
    assertTrue(handler.emit(twin, state, 2000).isPresent());
  }

  @Test
  void changedCallsignChangesSignatureAndEmitsImmediately() {
    Ais129810Handler handler =
        new Ais129810Handler(
            parserReturning(new byte[]{1}),
            AisClassBEmitterConfig.getDefaults(),
            10_000);
    DroneTwin twin = twin("CALL1");
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    twin.setCallSign("CALL2");
    assertTrue(handler.emit(twin, state, 1100).isPresent());
  }

  @Test
  void missingMmsiAndEmptyPayloadAreSuppressed() {
    N2kMessageParser parser = parserReturning(new byte[0]);
    Ais129810Handler handler =
        new Ais129810Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000);

    DroneTwin missing = new DroneTwin("missing");
    missing.setMmsi(null);
    assertTrue(handler.emit(missing, new DroneEmissionState(), 0).isEmpty());
    verifyNoInteractions(parser);

    DroneEmissionState state = new DroneEmissionState();
    assertTrue(handler.emit(twin("CALL"), state, 0).isEmpty());
    assertFalse(state.getOrCreateState(129810).isEmitted());
  }

  @Test
  void exposesPartBPgnAndName() {
    Ais129810Handler handler =
        new Ais129810Handler(
            mock(N2kMessageParser.class),
            AisClassBEmitterConfig.getDefaults(),
            1000);

    assertEquals(129810, handler.getPgn());
    assertTrue(handler.getName().contains("part B"));
  }

  private static DroneTwin twin(String callsign) {
    DroneTwin twin = new DroneTwin("vessel");
    twin.setMmsi(987654321L);
    twin.setCallSign(callsign);
    return twin;
  }

  private static N2kMessageParser parserReturning(byte[] payload) {
    N2kMessageParser parser = mock(N2kMessageParser.class);
    when(parser.encodeFromSource(eq(129810), any(FieldValueSource.class)))
        .thenReturn(payload);
    return parser;
  }
}
