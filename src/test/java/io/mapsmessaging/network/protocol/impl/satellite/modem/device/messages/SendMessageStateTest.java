package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendMessageStateTest {

  @Test
  void legacyStatusParsesReadyMessageDetails() {
    SendMessageState state =
        new SendMessageState("%MGRS: \"MSG1\",12.5,2,44,3,100,25", false);

    assertEquals("MSG1", state.getMessageName());
    assertEquals(12.5, state.getMessageNumber(), 0.0);
    assertEquals(2, state.getPriority());
    assertEquals(44, state.getSin());
    assertEquals(SendMessageState.State.TX_READY, state.getState());
    assertEquals(100, state.getLength());
    assertEquals(25, state.getBytesAcknowledged());
    assertEquals(0, state.getExpiry());
    assertTrue(state.isReady());
    assertFalse(state.isSending());
    assertFalse(state.isComplete());
    assertFalse(state.isFailed());
  }

  @Test
  void ogxStatusParsesSendingAndExpiryFields() {
    SendMessageState state =
        new SendMessageState("%MOQS: 1,\"OGX1\",2025-08-21 02:21:39,5,0,2,20,84,10", true);

    assertEquals("OGX1", state.getMessageName());
    assertEquals(0.0, state.getMessageNumber(), 0.0);
    assertEquals(1, state.getPriority());
    assertEquals(0, state.getSin());
    assertEquals(SendMessageState.State.TX_SENDING, state.getState());
    assertEquals(20, state.getExpiry());
    assertEquals(84, state.getLength());
    assertEquals(10, state.getBytesAcknowledged());
    assertTrue(state.isSending());
  }

  @Test
  void terminalAndUnknownStatesExposeClassification() {
    SendMessageState complete =
        new SendMessageState("%MGRS: msg,1,1,1,6,20,20", false);
    SendMessageState failed =
        new SendMessageState("%MGRS: msg,1,1,1,7,20,0", false);
    SendMessageState unknown =
        new SendMessageState("%MGRS: msg,1,1,1,99,20,0", false);

    assertTrue(complete.isComplete());
    assertFalse(complete.isFailed());
    assertTrue(failed.isFailed());
    assertEquals(SendMessageState.State.UNKNOWN, unknown.getState());
    assertFalse(unknown.isReady());
    assertFalse(unknown.isSending());
    assertFalse(unknown.isComplete());
    assertFalse(unknown.isFailed());
    assertTrue(complete.toString().contains("state=TX_COMPLETED"));
  }

  @Test
  void malformedStatusIsRejectedRatherThanSilentlyAccepted() {
    assertThrows(RuntimeException.class,
        () -> new SendMessageState("%MGRS: missing,fields", false));
  }
}
