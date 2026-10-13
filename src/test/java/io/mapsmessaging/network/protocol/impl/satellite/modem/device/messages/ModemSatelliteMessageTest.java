package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ModemSatelliteMessageTest {

  @ParameterizedTest
  @ValueSource(strings = {"at", "ogx", "xmodem"})
  void missing_payload_is_rejected_with_descriptive_state_error(String command) {
    ModemSatelliteMessage message = new ModemSatelliteMessage();
    message.setFormat(MessageFormat.HEX);
    IllegalStateException error = assertThrows(IllegalStateException.class, () -> encode(message, command));
    assertTrue(error.getMessage().contains("payload"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"at", "ogx"})
  void missing_format_is_rejected_with_descriptive_state_error(String command) {
    ModemSatelliteMessage message = new ModemSatelliteMessage();
    message.setPayload(new byte[0]);
    IllegalStateException error = assertThrows(IllegalStateException.class, () -> encode(message, command));
    assertTrue(error.getMessage().contains("format"));
  }

  private Object encode(ModemSatelliteMessage message, String command) {
    return switch (command) {
      case "at" -> message.toATCommand();
      case "ogx" -> message.toOgxCommand();
      default -> message.toOgxXModemCommand();
    };
  }

  @Test
  void legacyHexMessageDecodesMinAndPayload() {
    ModemSatelliteMessage message =
        new ModemSatelliteMessage("msg,x,x,10,2,x,2,14414243", false);

    assertEquals("msg", message.getName());
    assertEquals(10, message.getSin());
    assertEquals(20, message.getMin());
    assertEquals(2, message.getPriority());
    assertEquals(MessageFormat.HEX, message.getFormat());
    assertArrayEquals("ABC".getBytes(StandardCharsets.US_ASCII), message.getPayload());
  }

  @Test
  void ogxTextMessageUsesExplicitSinAndMinFields() {
    ModemSatelliteMessage message =
        new ModemSatelliteMessage("msg,2026-09-20,x,20,10,1,\"ABC\"", true);

    assertEquals("msg", message.getName());
    assertEquals("2026-09-20", message.getDatetime());
    assertEquals(10, message.getSin());
    assertEquals(20, message.getMin());
    assertEquals(MessageFormat.TEXT, message.getFormat());
    assertArrayEquals("ABC".getBytes(StandardCharsets.US_ASCII), message.getPayload());
  }

  @Test
  void outboundAtAndOgxCommandsEncodeConfiguredPayload() {
    ModemSatelliteMessage message = new ModemSatelliteMessage();
    message.setName("msg");
    message.setPriority(2);
    message.setSin(10);
    message.setMin(20);
    message.setFormat(MessageFormat.TEXT);
    message.setPayload("ABC".getBytes(StandardCharsets.US_ASCII));
    message.setMessageId(42);
    message.setLifeTime(15);

    assertEquals("\"msg\",2,10.20,1,\"ABC\"", message.toATCommand());
    assertEquals("42,2,15,5,1,\"\\0A\\14ABC\"", message.toOgxCommand());

    ModemSatelliteMessage.XmodemData xmodem = message.toOgxXModemCommand();
    assertArrayEquals(new byte[]{10,20,65,66,67}, xmodem.getData());
    assertTrue(xmodem.getCommand().startsWith("42,2,15,5,0,"));
    assertEquals(8, xmodem.getCommand().substring(xmodem.getCommand().lastIndexOf(',') + 1).length());
  }

  @Test
  void prefixedInboundLinesAreAccepted() {
    ModemSatelliteMessage message =
        new ModemSatelliteMessage("%MGFS:msg,x,x,10,2,x,2,1441", false);

    assertEquals("msg", message.getName());
    assertEquals(20, message.getMin());
    assertArrayEquals(new byte[]{0x41}, message.getPayload());
  }
}
