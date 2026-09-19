package io.mapsmessaging.state.n2k;

import io.mapsmessaging.network.protocol.impl.n2k.N2kProtocol;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.n2k.handler.AbstractDronePgnHandler;
import io.mapsmessaging.state.n2k.handler.PgnEmission;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DroneMonitorTest {

  @Test
  void emitsPayloadUsingInjectedClockWithoutCanHardware() throws Exception {
    TwinManager twinManager = mock(TwinManager.class);
    N2kProtocol protocol = mock(N2kProtocol.class);
    AbstractDronePgnHandler handler = mock(AbstractDronePgnHandler.class);
    DroneTwin twin = new DroneTwin("drone-one");
    byte[] payload = new byte[]{1, 2, 3};
    when(handler.emit(any(DroneTwin.class), any(DroneEmissionState.class), eq(1234L)))
        .thenReturn(Optional.of(new PgnEmission(129039, payload)));
    DroneMonitor monitor = new DroneMonitor(twinManager, protocol, List.of(handler), () -> 1234L);

    monitor.onTwinUpdated("drone-one", twin, null);

    verify(protocol).writePgn(129039, 0xff, payload);
  }

  @Test
  void handlerOrWriteFailureDoesNotPreventRemainingHandlers() throws Exception {
    TwinManager twinManager = mock(TwinManager.class);
    N2kProtocol protocol = mock(N2kProtocol.class);
    AbstractDronePgnHandler failingHandler = mock(AbstractDronePgnHandler.class);
    AbstractDronePgnHandler firstEmission = mock(AbstractDronePgnHandler.class);
    AbstractDronePgnHandler secondEmission = mock(AbstractDronePgnHandler.class);
    DroneTwin twin = new DroneTwin("drone-one");
    byte[] firstPayload = new byte[]{1};
    byte[] secondPayload = new byte[]{2};

    when(failingHandler.emit(any(), any(), anyLong())).thenThrow(new IllegalStateException("mapping failure"));
    when(firstEmission.emit(any(), any(), anyLong())).thenReturn(Optional.of(new PgnEmission(129039, firstPayload)));
    when(secondEmission.emit(any(), any(), anyLong())).thenReturn(Optional.of(new PgnEmission(129040, secondPayload)));
    doThrow(new IOException("write failure")).when(protocol).writePgn(129039, 0xff, firstPayload);
    DroneMonitor monitor = new DroneMonitor(
        twinManager,
        protocol,
        List.of(failingHandler, firstEmission, secondEmission),
        () -> 100L);

    monitor.onTwinUpdated("drone-one", twin, null);

    verify(protocol).writePgn(129039, 0xff, firstPayload);
    verify(protocol).writePgn(129040, 0xff, secondPayload);
  }

  @Test
  void ignoresNonDroneBlankIdentifierAndEmptyPayload() {
    TwinManager twinManager = mock(TwinManager.class);
    N2kProtocol protocol = mock(N2kProtocol.class);
    AbstractDronePgnHandler handler = mock(AbstractDronePgnHandler.class);
    EntityTwin otherTwin = mock(EntityTwin.class);
    DroneTwin droneTwin = new DroneTwin("drone-one");
    when(handler.emit(any(), any(), anyLong())).thenReturn(Optional.of(new PgnEmission(129039, new byte[0])));
    DroneMonitor monitor = new DroneMonitor(twinManager, protocol, List.of(handler), () -> 100L);

    monitor.onTwinUpdated("other", otherTwin, null);
    monitor.onTwinUpdated(" ", droneTwin, null);
    monitor.onTwinUpdated("drone-one", droneTwin, null);

    verify(handler).emit(eq(droneTwin), any(), eq(100L));
    verifyNoInteractions(protocol);
  }

  @Test
  void removalAndCloseDiscardPerDroneEmissionState() {
    TwinManager twinManager = mock(TwinManager.class);
    N2kProtocol protocol = mock(N2kProtocol.class);
    AbstractDronePgnHandler handler = mock(AbstractDronePgnHandler.class);
    DroneTwin twin = new DroneTwin("drone-one");
    List<DroneEmissionState> observedStates = new ArrayList<>();
    doAnswer(invocation -> {
      observedStates.add(invocation.getArgument(1));
      return Optional.empty();
    }).when(handler).emit(any(), any(), anyLong());
    DroneMonitor monitor = new DroneMonitor(twinManager, protocol, List.of(handler), () -> 100L);

    monitor.onTwinUpdated("drone-one", twin, null);
    monitor.onTwinRemoved(twin, null);
    monitor.onTwinUpdated("drone-one", twin, null);
    monitor.close();
    monitor.onTwinUpdated("drone-one", twin, null);

    assertEquals(3, observedStates.size());
    assertNotSame(observedStates.get(0), observedStates.get(1));
    assertNotSame(observedStates.get(1), observedStates.get(2));
    verify(twinManager).removeObserver(monitor);
  }
}
