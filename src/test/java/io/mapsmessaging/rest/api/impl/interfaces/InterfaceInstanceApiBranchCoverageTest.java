package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterfaceInstanceApiBranchCoverageTest {

  @Test
  void endpointParserAcceptsTrimmedUuidAndRejectsInvalidValues() throws Exception {
    InterfaceInstanceApi api = new InterfaceInstanceApi();
    Method parse = InterfaceInstanceApi.class.getDeclaredMethod("parseEndpointId", String.class);
    parse.setAccessible(true);
    UUID uuid = UUID.randomUUID();

    assertEquals(uuid, parse.invoke(api, " " + uuid + " "));
    assertNull(parse.invoke(api, new Object[]{null}));
    assertNull(parse.invoke(api, " "));
    assertNull(parse.invoke(api, "invalid"));
  }

  @Test
  void requestedStateMappingIsCaseInsensitiveAndRejectsUnknownValues() throws Exception {
    InterfaceInstanceApi api = new InterfaceInstanceApi();
    Method map = InterfaceInstanceApi.class.getDeclaredMethod("mapRequestedState", String.class);
    map.setAccessible(true);

    assertEquals(STATE.STOPPED, map.invoke(api, " STOPPED "));
    assertEquals(STATE.START, map.invoke(api, "started"));
    assertEquals(STATE.PAUSED, map.invoke(api, "Paused"));
    assertEquals(STATE.RESUME, map.invoke(api, "RESUMED"));
    assertNull(map.invoke(api, "other"));
  }

  @Test
  void applyStatePerformsValidTransitionsAndReportsNoChangeOtherwise() throws Exception {
    InterfaceInstanceApi api = new InterfaceInstanceApi();
    Method apply = InterfaceInstanceApi.class.getDeclaredMethod(
        "applyState", STATE.class, EndPointManager.class);
    apply.setAccessible(true);
    EndPointManager manager = mock(EndPointManager.class);

    when(manager.getState()).thenReturn(STATE.STOPPED);
    Response started = (Response) apply.invoke(api, STATE.START, manager);
    verify(manager).start();
    assertEquals("Success", ((StatusResponse) started.getEntity()).getStatus());

    reset(manager);
    when(manager.getState()).thenReturn(STATE.START);
    Response paused = (Response) apply.invoke(api, STATE.PAUSED, manager);
    verify(manager).pause();
    assertEquals("Success", ((StatusResponse) paused.getEntity()).getStatus());

    reset(manager);
    when(manager.getState()).thenReturn(STATE.START);
    Response unchanged = (Response) apply.invoke(api, STATE.RESUME, manager);
    assertEquals("No change", ((StatusResponse) unchanged.getEntity()).getStatus());
  }
}