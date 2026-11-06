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

class Ais129809HandlerTest {

  @Test
  void staticPartAEmitsThenSuppressesUnchangedSignatureUntilInterval() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setName("Vessel A");
    Ais129809Handler handler =
        new Ais129809Handler(parserReturning(new byte[]{9}), config, 1000);
    DroneTwin twin = new DroneTwin("vessel");
    twin.setMmsi(123456789L);
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    assertTrue(handler.emit(twin, state, 1200).isEmpty());
    assertTrue(handler.emit(twin, state, 2000).isPresent());
  }

  @Test
  void changedStaticSignatureEmitsImmediately() {
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setName("Before");
    Ais129809Handler handler =
        new Ais129809Handler(parserReturning(new byte[]{1}), config, 10_000);
    DroneTwin twin = new DroneTwin("vessel");
    twin.setMmsi(123456789L);
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    config.setName("After");
    assertTrue(handler.emit(twin, state, 1100).isPresent());
  }

  @Test
  void missingMmsiOrEmptyPayloadDoesNotRecordEmission() {
    N2kMessageParser parser = parserReturning(new byte[0]);
    Ais129809Handler handler =
        new Ais129809Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000);

    DroneTwin missing = new DroneTwin("missing");
    missing.setMmsi(null);
    assertTrue(handler.emit(missing, new DroneEmissionState(), 0).isEmpty());
    verifyNoInteractions(parser);

    DroneEmissionState state = new DroneEmissionState();
    DroneTwin valid = new DroneTwin("valid");
    valid.setMmsi(123L);
    assertTrue(handler.emit(valid, state, 0).isEmpty());
    assertFalse(state.getOrCreateState(129809).isEmitted());
  }

  @Test
  void exposesPartAPgnAndName() {
    Ais129809Handler handler =
        new Ais129809Handler(
            mock(N2kMessageParser.class),
            AisClassBEmitterConfig.getDefaults(),
            1000);

    assertEquals(129809, handler.getPgn());
    assertTrue(handler.getName().contains("part A"));
  }

  private static N2kMessageParser parserReturning(byte[] payload) {
    N2kMessageParser parser = mock(N2kMessageParser.class);
    when(parser.encodeFromSource(eq(129809), any(FieldValueSource.class)))
        .thenReturn(payload);
    return parser;
  }
}
