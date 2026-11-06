package io.mapsmessaging.state.n2k;

import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.network.protocol.impl.n2k.N2kProtocol;
import io.mapsmessaging.state.config.n2k.N2KAisConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KPgnTransmitConfigDTO;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinLifecycleStatus;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DroneMonitorTest {

  @Test
  void eligibleDroneUpdateEmitsConfiguredPgn() throws Exception {
    TwinManager twinManager = mock(TwinManager.class);
    N2kMessageParser parser = mock(N2kMessageParser.class);
    N2kProtocol protocol = mock(N2kProtocol.class);
    when(parser.encodeFromSource(eq(129039), any(FieldValueSource.class)))
        .thenReturn(new byte[]{1, 2});

    DroneMonitor monitor =
        new DroneMonitor(twinManager, onlyPositionPgn(100_000), parser, protocol);

    monitor.onTwinUpdated("drone-1", eligibleTwin("drone-1"), null);

    verify(protocol).writePgn(129039, 0xff, new byte[]{1, 2});
  }

  @Test
  void removalClearsPerDroneEmissionState() throws Exception {
    TwinManager twinManager = mock(TwinManager.class);
    N2kMessageParser parser = mock(N2kMessageParser.class);
    N2kProtocol protocol = mock(N2kProtocol.class);
    when(parser.encodeFromSource(eq(129039), any(FieldValueSource.class)))
        .thenReturn(new byte[]{7});

    DroneMonitor monitor =
        new DroneMonitor(twinManager, onlyPositionPgn(100_000), parser, protocol);
    DroneTwin twin = eligibleTwin("drone-2");

    monitor.onTwinUpdated("drone-2", twin, null);
    monitor.onTwinUpdated("drone-2", twin, null);
    verify(protocol, times(1)).writePgn(eq(129039), eq(0xff), any(byte[].class));

    monitor.onTwinRemoved(twin, null);
    monitor.onTwinUpdated("drone-2", twin, null);

    verify(protocol, times(2)).writePgn(eq(129039), eq(0xff), any(byte[].class));
  }

  @Test
  void nonDroneAndBlankIdentifiersAreIgnored() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class);
    DroneMonitor monitor =
        new DroneMonitor(
            mock(TwinManager.class),
            onlyPositionPgn(1000),
            mock(N2kMessageParser.class),
            protocol
        );

    monitor.onTwinUpdated("entity", mock(EntityTwin.class), null);
    monitor.onTwinUpdated(" ", eligibleTwin("drone-3"), null);

    verifyNoInteractions(protocol);
  }

  @Test
  void closeUnregistersObserver() {
    TwinManager twinManager = mock(TwinManager.class);
    DroneMonitor monitor =
        new DroneMonitor(
            twinManager,
            onlyPositionPgn(1000),
            mock(N2kMessageParser.class),
            mock(N2kProtocol.class)
        );

    monitor.close();

    verify(twinManager).removeObserver(monitor);
  }

  private static N2KAisConfigDTO onlyPositionPgn(long interval) {
    N2KAisConfigDTO config = new N2KAisConfigDTO();
    config.setPgn129039(new N2KPgnTransmitConfigDTO(true, interval));
    config.setPgn129040(new N2KPgnTransmitConfigDTO(false, 1000));
    config.setPgn129809(new N2KPgnTransmitConfigDTO(false, 1000));
    config.setPgn129810(new N2KPgnTransmitConfigDTO(false, 1000));
    return config;
  }

  private static DroneTwin eligibleTwin(String id) {
    DroneTwin twin = new DroneTwin(id);
    twin.setMmsi(123456789L);
    twin.setLifecycleStatus(TwinLifecycleStatus.ACTIVE);
    twin.setGpsValid(true);
    twin.setNavigationUpdatedAt(Instant.now());
    twin.setGeoPosition(new GeoPosition(38.4, -9.1, 0.0, null, null));
    twin.setCourseOverGroundDegrees(10.0);
    twin.setHeadingDegrees(10.0);
    twin.setGroundSpeedMetersPerSecond(2.0);
    return twin;
  }
}
