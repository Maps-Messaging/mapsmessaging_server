package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.IncomingMessageDetails;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdpModemProtocolTest {

  @Test
  void sendMessageBuildsLegacyAtCommandAndTracksPayloadBytes() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand(anyString()))
        .thenReturn(CompletableFuture.completedFuture("OK"));

    ModemSatelliteMessage message = new ModemSatelliteMessage();
    message.setName("MSG1");
    message.setPriority(2);
    message.setSin(10);
    message.setMin(20);
    message.setFormat(MessageFormat.TEXT);
    message.setPayload("ABC".getBytes(StandardCharsets.US_ASCII));

    IdpModemProtocol protocol = new IdpModemProtocol(modem);
    protocol.sendMessage(message);

    verify(modem).sendATCommand("AT%MGRT=\"MSG1\",2,10.20,1,\"ABC\"");
    assertEquals(3, protocol.getSentBytes().get());
    assertEquals(1, protocol.getSentPackets().get());
  }

  @Test
  void networkStatusUsesFirstResponseLine() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand("ATS54?"))
        .thenReturn(CompletableFuture.completedFuture("5\r\nOK"));

    NetworkStatus status = new IdpModemProtocol(modem).getCurrentNetworkStatus();

    assertTrue(status.canSend());
  }

  @Test
  void deleteSentMessageQuotesNameExactlyOnce() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand(anyString()))
        .thenReturn(CompletableFuture.completedFuture("OK"));
    IdpModemProtocol protocol = new IdpModemProtocol(modem);

    assertTrue(protocol.deleteSentMessages("MSG1").join());
    assertTrue(protocol.deleteSentMessages("\"MSG2\"").join());

    verify(modem).sendATCommand("AT%MGRD=\"MSG1\"");
    verify(modem).sendATCommand("AT%MGRD=\"MSG2\"");
  }

  @Test
  void incomingMessageUsesRequestedIdAndBase64Format() {
    Modem modem = mock(Modem.class);
    when(modem.sendATCommand("AT%MGFG=id-1,3"))
        .thenReturn(CompletableFuture.completedFuture("msg,x,x,10,2,x,2,1441\r\nOK"));
    IncomingMessageDetails details = mock(IncomingMessageDetails.class);
    when(details.getId()).thenReturn("id-1");

    ModemSatelliteMessage message =
        new IdpModemProtocol(modem).getMessage(details).join();

    assertNotNull(message);
    verify(modem).sendATCommand("AT%MGFG=id-1,3");
    assertEquals("IDG mode modem", new IdpModemProtocol(modem).getType());
  }
}
