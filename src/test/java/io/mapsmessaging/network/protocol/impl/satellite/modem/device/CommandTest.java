package io.mapsmessaging.network.protocol.impl.satellite.modem.device;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest {

  @Test
  void commandRetainsTextFutureAndMutableTimeout() {
    CompletableFuture<String> future = new CompletableFuture<>();
    Command command = new Command("AT+CSQ", future);

    assertEquals("AT+CSQ", command.cmd);
    assertSame(future, command.future);
    assertEquals(0L, command.timeout);

    command.timeout = 1234L;
    assertEquals(1234L, command.timeout);

    future.complete("OK");
    assertEquals("OK", command.future.join());
  }
}