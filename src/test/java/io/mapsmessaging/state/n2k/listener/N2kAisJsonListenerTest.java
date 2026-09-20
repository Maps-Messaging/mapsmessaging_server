package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class N2kAisJsonListenerTest {

  @Test
  void listenerReportsConfiguredAisPgn() {
    assertEquals(129039, new N2kAisJsonListener(129039).getPgn());
    assertEquals(129810, new N2kAisJsonListener(129810).getPgn());
  }

  @Test
  void aisContactPacketDoesNotMutateLocalDroneTwin() {
    DroneTwin twin = mock(DroneTwin.class);
    TwinUpdateContext context = mock(TwinUpdateContext.class);

    new N2kAisJsonListener(129039).handle(twin, new JsonObject(), context);

    verifyNoInteractions(twin, context);
  }
}
