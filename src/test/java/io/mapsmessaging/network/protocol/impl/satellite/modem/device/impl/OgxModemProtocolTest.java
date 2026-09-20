package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OgxModemProtocolTest {

  @Test
  void smallOutboundMessageUsesDirectMomtCommandAndTracksBytes() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand(anyString()))
        .thenReturn(CompletableFuture.completedFuture("OK"));

    ModemSatelliteMessage message = new ModemSatelliteMessage();
    message.setMessageId(7);
    message.setLifeTime(10);
    message.setSin(10);
    message.setMin(20);
    message.setFormat(MessageFormat.TEXT);
    message.setPayload("ABC".getBytes(StandardCharsets.US_ASCII));

    OgxModemProtocol protocol = new OgxModemProtocol(modem);
    protocol.sendMessage(message);

    verify(modem).sendATCommand(startsWith("AT%MOMT=7,2,10,5,1,"));
    assertEquals(3, protocol.getSentBytes().get());
    assertEquals(1, protocol.getSentPackets().get());
  }

  @Test
  void networkStatusUsesFirstNetinfoLine() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand("AT%NETINFO"))
        .thenReturn(CompletableFuture.completedFuture("%NETINFO: 2,5,0,0,0\r\nOK"));

    NetworkStatus status = new OgxModemProtocol(modem).getCurrentNetworkStatus();

    assertTrue(status.canSend());
  }

  @Test
  void deleteResultsReflectModemResponse() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand("AT%MOMD=1"))
        .thenReturn(CompletableFuture.completedFuture("OK"));
    when(modem.sendATCommand("AT%MTMD=2"))
        .thenReturn(CompletableFuture.completedFuture("ERROR"));

    OgxModemProtocol protocol = new OgxModemProtocol(modem);

    assertTrue(protocol.deleteSentMessages("1").join());
    assertFalse(protocol.deleteIncomingMessage("2").join());
  }

  @Test
  void xmodemOkResponseCompletesWithNoPayload() {
    CompletableFuture<byte[]> future =
        new OgxModemProtocol(mock(Modem.class)).processXmodemRequest("%MTMG: OK");

    assertTrue(future.isDone());
    assertNull(future.join());
  }
}
