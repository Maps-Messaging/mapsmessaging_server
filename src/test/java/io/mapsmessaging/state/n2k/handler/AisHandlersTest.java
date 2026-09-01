package io.mapsmessaging.state.n2k.handler;

import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.n2k.DroneEmissionState;
import io.mapsmessaging.state.n2k.msg.AisClassBEmitterConfig;
import io.mapsmessaging.state.n2k.msg.source.AisClassBExtendedPositionFieldValueSource;
import io.mapsmessaging.state.n2k.msg.source.AisClassBPositionFieldValueSource;
import io.mapsmessaging.state.n2k.msg.source.AisClassBStaticDataPartAFieldValueSource;
import io.mapsmessaging.state.n2k.msg.source.AisClassBStaticDataPartBFieldValueSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AisHandlersTest {

  private static final byte[] PAYLOAD = new byte[]{0x11, 0x22};

  @Test
  void positionHandlerEmitsPgn129039UsingPositionReportSource() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129039Handler handler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);

    Optional<PgnEmission> emission = handler.emit(validTwin(), new DroneEmissionState(), 100L);

    assertTrue(emission.isPresent());
    assertEquals(129039, emission.get().getPgn());
    assertArrayEquals(PAYLOAD, emission.get().getPayload());
    assertEncodedSource(parser, 129039, AisClassBPositionFieldValueSource.class);
  }

  @Test
  void extendedHandlerEmitsPgn129040UsingExtendedReportSource() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129040Handler handler = new Ais129040Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);

    Optional<PgnEmission> emission = handler.emit(validTwin(), new DroneEmissionState(), 100L);

    assertTrue(emission.isPresent());
    assertEquals(129040, emission.get().getPgn());
    assertEncodedSource(parser, 129040, AisClassBExtendedPositionFieldValueSource.class);
  }

  @Test
  void staticPartAHandlerEmitsPgn129809UsingPartASource() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129809Handler handler = new Ais129809Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);

    Optional<PgnEmission> emission = handler.emit(validTwin(), new DroneEmissionState(), 100L);

    assertTrue(emission.isPresent());
    assertEquals(129809, emission.get().getPgn());
    assertEncodedSource(parser, 129809, AisClassBStaticDataPartAFieldValueSource.class);
  }

  @Test
  void staticPartBHandlerEmitsPgn129810UsingPartBSource() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129810Handler handler = new Ais129810Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);

    Optional<PgnEmission> emission = handler.emit(validTwin(), new DroneEmissionState(), 100L);

    assertTrue(emission.isPresent());
    assertEquals(129810, emission.get().getPgn());
    assertEncodedSource(parser, 129810, AisClassBStaticDataPartBFieldValueSource.class);
  }

  @Test
  void dynamicHandlersRequireActiveValidFreshPosition() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129039Handler positionHandler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    Ais129040Handler extendedHandler = new Ais129040Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneTwin twin = validTwin();
    twin.setGpsValid(false);

    assertTrue(positionHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());
    assertTrue(extendedHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());

    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(null);
    assertTrue(positionHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());
    assertTrue(extendedHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());

    twin.setNavigationUpdatedAt(Instant.EPOCH);
    twin.setLifecycleStatus(TwinLifecycleStatus.STALE);
    assertTrue(positionHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());
    assertTrue(extendedHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());
    verifyNoInteractions(parser);
  }

  @Test
  void staticHandlersRequireMmsiAndAcceptPartiallyPopulatedTwin() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129809Handler partAHandler = new Ais129809Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    Ais129810Handler partBHandler = new Ais129810Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneTwin twin = new DroneTwin("static-twin");
    twin.setMmsi(null);

    assertTrue(partAHandler.emit(null, new DroneEmissionState(), 0L).isEmpty());
    assertTrue(partBHandler.emit(null, new DroneEmissionState(), 0L).isEmpty());
    assertTrue(partAHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());
    assertTrue(partBHandler.emit(twin, new DroneEmissionState(), 0L).isEmpty());
    verifyNoInteractions(parser);
  }

  @Test
  void positionHandlerSuppressesEarlyChangeThenEmitsAtHalfInterval() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129039Handler handler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();

    assertTrue(handler.emit(twin, state, 1000L).isPresent());
    twin.getGeoPosition().setLatitude(twin.getGeoPosition().getLatitude() + 0.0001d);
    assertTrue(handler.emit(twin, state, 1200L).isEmpty());
    assertTrue(handler.emit(twin, state, 1500L).isPresent());
  }

  @Test
  void positionHandlerSuppressesNoChangeAndEmitsPeriodically() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129039Handler handler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();

    assertTrue(handler.emit(twin, state, 1000L).isPresent());
    assertTrue(handler.emit(twin, state, 1499L).isEmpty());
    assertTrue(handler.emit(twin, state, 2000L).isPresent());
  }

  @Test
  void headingAndCourseWrapAroundDoNotCreateFalseMaterialChange() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129039Handler handler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();
    twin.setHeadingDegrees(358.0d);
    twin.setCourseOverGroundDegrees(358.0d);

    assertTrue(handler.emit(twin, state, 0L).isPresent());
    twin.setHeadingDegrees(2.0d);
    twin.setCourseOverGroundDegrees(2.0d);

    assertTrue(handler.emit(twin, state, 500L).isEmpty());
  }

  @Test
  void unchangedNullMotionDoesNotCreateFalseMaterialChange() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129039Handler handler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();
    twin.setHeadingDegrees(null);
    twin.setCourseOverGroundDegrees(null);
    twin.setGroundSpeedMetersPerSecond(null);

    assertTrue(handler.emit(twin, state, 0L).isPresent());
    assertTrue(handler.emit(twin, state, 500L).isEmpty());
  }

  @Test
  void extendedHandlerUsesSameHalfIntervalChangeThrottle() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129040Handler handler = new Ais129040Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();

    assertTrue(handler.emit(twin, state, 0L).isPresent());
    twin.getGeoPosition().setLatitude(twin.getGeoPosition().getLatitude() + 0.0001d);
    assertTrue(handler.emit(twin, state, 100L).isEmpty());
    assertTrue(handler.emit(twin, state, 500L).isPresent());
  }

  @Test
  void failedEncodingDoesNotAdvanceEmissionState() {
    N2kMessageParser parser = mock(N2kMessageParser.class);
    when(parser.encodeFromSource(anyInt(), any(FieldValueSource.class)))
        .thenReturn(new byte[0])
        .thenReturn(PAYLOAD);
    Ais129039Handler handler = new Ais129039Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();

    assertTrue(handler.emit(twin, state, 0L).isEmpty());
    assertTrue(handler.emit(twin, state, 1L).isPresent());
  }

  @Test
  void staticPartAEmitsOnSignatureChangeAndPeriodicDeadline() {
    N2kMessageParser parser = parserReturningPayload();
    Ais129809Handler handler = new Ais129809Handler(parser, AisClassBEmitterConfig.getDefaults(), 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();

    assertTrue(handler.emit(twin, state, 0L).isPresent());
    assertTrue(handler.emit(twin, state, 1L).isEmpty());
    twin.setDisplayName("renamed vessel");
    assertTrue(handler.emit(twin, state, 2L).isPresent());
    assertTrue(handler.emit(twin, state, 1002L).isPresent());
  }

  @Test
  void staticPartBHandlesNullStringsConsistently() {
    N2kMessageParser parser = parserReturningPayload();
    AisClassBEmitterConfig config = AisClassBEmitterConfig.getDefaults();
    config.setVendorId(null);
    config.setCallsign(null);
    Ais129810Handler handler = new Ais129810Handler(parser, config, 1000L);
    DroneEmissionState state = new DroneEmissionState();
    DroneTwin twin = validTwin();
    twin.setCallSign(null);

    assertTrue(handler.emit(twin, state, 0L).isPresent());
    assertTrue(handler.emit(twin, state, 999L).isEmpty());
    assertTrue(handler.emit(twin, state, 1000L).isPresent());
  }

  private static N2kMessageParser parserReturningPayload() {
    N2kMessageParser parser = mock(N2kMessageParser.class);
    when(parser.encodeFromSource(anyInt(), any(FieldValueSource.class))).thenReturn(PAYLOAD);
    return parser;
  }

  private static void assertEncodedSource(N2kMessageParser parser, int pgn, Class<? extends FieldValueSource> expectedType) {
    ArgumentCaptor<FieldValueSource> sourceCaptor = ArgumentCaptor.forClass(FieldValueSource.class);
    verify(parser).encodeFromSource(org.mockito.ArgumentMatchers.eq(pgn), sourceCaptor.capture());
    assertInstanceOf(expectedType, sourceCaptor.getValue());
  }

  private static DroneTwin validTwin() {
    DroneTwin twin = new DroneTwin("drone-one");
    twin.setMmsi(999_001_234L);
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setGeoPosition(new GeoPosition(-33.8688d, 151.2093d, null, null));
    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(Instant.parse("2026-07-29T01:00:07Z"));
    twin.setHeadingDegrees(180.0d);
    twin.setCourseOverGroundDegrees(181.0d);
    twin.setGroundSpeedMetersPerSecond(2.0d);
    twin.setDisplayName("drone one");
    twin.setCallSign("DRONE1");
    return twin;
  }
}
