/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public abstract class MemoryEndPoint extends EndPoint {

  private static final long IDLE_POLL_NANOS = 100_000L;

  protected final MemoryTransport transport;
  private final AtomicBoolean running;
  private final AtomicReference<Selectable> readSelectable;
  private final AtomicReference<Selectable> writeSelectable;
  private final Thread readinessThread;

  protected MemoryEndPoint(long id, EndPointServerStatus server, MemoryTransport transport) {
    super(id, server);
    this.transport = transport;
    running = new AtomicBoolean(true);
    readSelectable = new AtomicReference<>();
    writeSelectable = new AtomicReference<>();
    name = getProtocol() + "_" + transport.remoteAddress();
    readinessThread = Thread.ofVirtual().name(name + "-readiness").start(this::pollReadiness);
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    int count = transport.write(packet.getRawBuffer());
    if (count > 0) {
      updateWriteBytes(count);
    }
    return count;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int count = transport.read(packet.getRawBuffer());
    if (count > 0) {
      updateReadBytes(count);
    }
    return count;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    if (isClosed()) {
      throw new ClosedChannelException();
    }
    if ((selectionKey & SelectionKey.OP_READ) != 0) {
      readSelectable.set(runner);
    }
    if ((selectionKey & SelectionKey.OP_WRITE) != 0) {
      writeSelectable.set(runner);
    }
    return completedTask();
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    if ((selectionKey & SelectionKey.OP_READ) != 0 || selectionKey < 0) {
      readSelectable.set(null);
    }
    if ((selectionKey & SelectionKey.OP_WRITE) != 0 || selectionKey < 0) {
      writeSelectable.set(null);
    }
    return completedTask();
  }

  @Override
  public String getAuthenticationConfig() {
    return null;
  }

  @Override
  public String getRemoteSocketAddress() {
    return transport.remoteAddress();
  }

  @Override
  public void close() throws IOException {
    if (running.getAndSet(false)) {
      readinessThread.interrupt();
      transport.close();
      super.close();
    }
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(getClass().getName() + "_" + getId());
  }

  private void pollReadiness() {
    while (running.get()) {
      boolean signalled = false;
      Selectable reader = readSelectable.get();
      if (reader != null && transport.hasData()) {
        reader.selected(reader, null, SelectionKey.OP_READ);
        signalled = true;
      }
      Selectable writer = writeSelectable.get();
      if (writer != null && transport.canWrite()) {
        writer.selected(writer, null, SelectionKey.OP_WRITE);
        signalled = true;
      }
      if (!signalled) {
        LockSupport.parkNanos(IDLE_POLL_NANOS);
      }
    }
  }

  private static FutureTask<SelectionKey> completedTask() {
    FutureTask<SelectionKey> task = new FutureTask<>(() -> null);
    task.run();
    return task;
  }
}
