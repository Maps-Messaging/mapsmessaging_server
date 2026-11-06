package io.mapsmessaging.state.n2k.handler;

import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.n2k.DroneEmissionState;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Ais129039HandlerTest {

  @Test
  void firstEligiblePositionEmitsAndRecordsSnapshot() {
    N2kMessageParser parser = parserReturning(new byte[]{1, 2, 3});
    Ais129039Handler handler =
        new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000);
    DroneTwin twin = eligibleTwin();
    DroneEmissionState emissionState = new DroneEmissionState();

    PgnEmission emission = handler.emit(twin, emissionState, 1000).orElseThrow();

    assertEquals(129039, emission.getPgn());
    assertArrayEquals(new byte[]{1, 2, 3}, emission.getPayload());

    PgnEmissionState state = emissionState.getOrCreateState(129039);
    assertTrue(state.isEmitted());
    assertEquals(1000, state.getLastEmitAt());
    assertEquals(twin.getGeoPosition().getLatitude(), state.getLastLatitude());
    assertEquals(twin.getGeoPosition().getLongitude(), state.getLastLongitude());
  }

  @Test
  void intervalAndMaterialMotionControlSubsequentEmission() {
    Ais129039Handler handler =
        new Ais129039Handler(
            parserReturning(new byte[]{7}),
            AisClassBEmitterConfig.getDefaults(),
            1000);
    DroneTwin twin = eligibleTwin();
    DroneEmissionState state = new DroneEmissionState();

    assertTrue(handler.emit(twin, state, 1000).isPresent());
    assertTrue(handler.emit(twin, state, 1200).isEmpty());

    twin.getGeoPosition().setLatitude(38.4001);
    assertTrue(handler.emit(twin, state, 1600).isPresent());
  }

  @Test
  void invalidTwinOrEmptyEncodedPayloadDoesNotAdvanceEmissionState() {
    N2kMessageParser parser = parserReturning(new byte[0]);
    Ais129039Handler handler =
        new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000);
    DroneEmissionState state = new DroneEmissionState();

    DroneTwin invalid = eligibleTwin();
    invalid.setGpsValid(false);
    assertTrue(handler.emit(invalid, state, 1000).isEmpty());
    verifyNoInteractions(parser);

    DroneTwin valid = eligibleTwin();
    assertTrue(handler.emit(valid, state, 1000).isEmpty());
    assertFalse(state.getOrCreateState(129039).isEmitted());
  }

  @Test
  void exposesCorrectPgnAndDescription() {
    Ais129039Handler handler =
        new Ais129039Handler(
            mock(N2kMessageParser.class),
            AisClassBEmitterConfig.getDefaults(),
            1000);

    assertEquals(129039, handler.getPgn());
    assertEquals("AIS class B position report", handler.getName());
  }

  private static N2kMessageParser parserReturning(byte[] payload) {
    N2kMessageParser parser = mock(N2kMessageParser.class);
    when(parser.encodeFromSource(eq(129039), any(FieldValueSource.class)))
        .thenReturn(payload);
    return parser;
  }

  static DroneTwin eligibleTwin() {
    DroneTwin twin = new DroneTwin("vessel-1");
    twin.setMmsi(123456789L);
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 0.0, null, null));
    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(Instant.ofEpochSecond(65));
    twin.setCourseOverGroundDegrees(10.0);
    twin.setHeadingDegrees(350.0);
    twin.setGroundSpeedMetersPerSecond(4.2);
    twin.setDisplayName("Survey Vessel");
    twin.setCallSign("SV001");
    return twin;
  }
}
