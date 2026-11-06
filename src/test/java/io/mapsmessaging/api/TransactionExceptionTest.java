package io.mapsmessaging.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TransactionExceptionTest {

  @Test
  void transactionExceptionPreservesMessageAndIOExceptionContract() {
    TransactionException exception = new TransactionException("transaction already completed");

    assertInstanceOf(IOException.class, exception);
    assertEquals("transaction already completed", exception.getMessage());
  }
}