package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SatelliteEndPointTest {

  @Test
  void identityAndProtocolComeFromTerminal() {
    SatelliteEndPointServer server = mock(SatelliteEndPointServer.class);
    RemoteDeviceInfo terminal = terminal("terminal-1");
    SatelliteEndPoint endpoint = new SatelliteEndPoint(1L, server, terminal);

    assertEquals("terminal-1", endpoint.getName());
    assertEquals("satellite", endpoint.getProtocol());
    assertSame(terminal, endpoint.getTerminalInfo());
  }

  @Test
  void sendMuteAndUnmuteDelegateToServerUsingTerminalId() throws Exception {
    SatelliteEndPointServer server = mock(SatelliteEndPointServer.class);
    SatelliteEndPoint endpoint =
        new SatelliteEndPoint(1L, server, terminal("terminal-2"));
    MessageData message = new MessageData();

    endpoint.sendMessage(message);
    endpoint.mute();
    endpoint.unmute();

    verify(server).sendClientMessage("terminal-2", message);
    verify(server).mute("terminal-2");
    verify(server).unmute("terminal-2");
  }

  @Test
  void terminalRefreshOnlyAcceptsMatchingIdentity() throws Exception {
    SatelliteEndPointServer server = mock(SatelliteEndPointServer.class);
    RemoteDeviceInfo original = terminal("terminal-3");
    SatelliteEndPoint endpoint = new SatelliteEndPoint(1L, server, original);
    RemoteDeviceInfo replacement = terminal("terminal-3");
    when(server.updateTerminalInfo("terminal-3")).thenReturn(replacement);

    endpoint.updateTerminalInfo();
    assertSame(replacement, endpoint.getTerminalInfo());

    RemoteDeviceInfo wrong = terminal("different");
    when(server.updateTerminalInfo("terminal-3")).thenReturn(wrong);
    endpoint.updateTerminalInfo();

    assertSame(replacement, endpoint.getTerminalInfo());
  }

  private static RemoteDeviceInfo terminal(String id) {
    RemoteDeviceInfo info = mock(RemoteDeviceInfo.class);
    when(info.getUniqueId()).thenReturn(id);
    return info;
  }
}
