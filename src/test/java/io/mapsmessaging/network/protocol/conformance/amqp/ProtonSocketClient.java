/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.amqp;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Transport;

final class ProtonSocketClient implements AutoCloseable {

  private final Socket socket;
  private final Transport transport;
  private final Connection connection;

  ProtonSocketClient(String host, int port) throws Exception {
    socket = new Socket(host, port);
    socket.setSoTimeout(100);

    transport = Proton.transport();
    connection = Proton.connection();
    connection.setContainer("maps-conformance-" + UUID.randomUUID());
    transport.bind(connection);
    connection.open();

    pumpUntil(() -> connection.getRemoteState() == EndpointState.ACTIVE, 5_000);
  }

  Connection connection() {
    return connection;
  }

  void pumpUntil(BooleanSupplier condition, long timeoutMillis) throws Exception {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
      pumpOnce();
    }
    if (!condition.getAsBoolean()) {
      throw new AssertionError("Timed out waiting for AMQP peer state");
    }
  }

  void pumpOnce() throws Exception {
    flushOutput();

    ByteBuffer inputBuffer = transport.getInputBuffer();
    if (inputBuffer.hasRemaining()) {
      byte[] incoming = new byte[Math.min(inputBuffer.remaining(), 8192)];
      try {
        int count = socket.getInputStream().read(incoming);
        if (count < 0) {
          return;
        }
        if (count > 0) {
          inputBuffer.put(incoming, 0, count);
          transport.processInput().checkIsOk();
        }
      } catch (SocketTimeoutException ignored) {
        // No peer input this cycle.
      }
    }

    flushOutput();
  }

  private void flushOutput() throws IOException {
    ByteBuffer output = transport.getOutputBuffer();
    if (output != null && output.hasRemaining()) {
      byte[] encoded = new byte[output.remaining()];
      output.get(encoded);
      socket.getOutputStream().write(encoded);
      socket.getOutputStream().flush();
      transport.outputConsumed();
    }
  }

  @Override
  public void close() throws Exception {
    if (connection.getLocalState() != EndpointState.CLOSED) {
      connection.close();
      try {
        pumpUntil(() -> connection.getRemoteState() == EndpointState.CLOSED, 1_000);
      } catch (AssertionError ignored) {
        // A peer can close transport aggressively; the conformance tests assert lifecycle separately.
      }
    }
    socket.close();
  }
}
