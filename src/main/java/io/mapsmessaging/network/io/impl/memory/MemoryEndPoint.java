/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.memory.MemoryConnection;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.BufferedReadEndPoint;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/** A managed shared-memory or RDMA stream, independent of TCP endpoint implementation. */
public class MemoryEndPoint extends EndPoint implements BufferedReadEndPoint {
  private final MemoryConnection connection;
  private final MemoryReadiness readiness;
  private final EndPointJMX mbean;
  private final AtomicBoolean closed = new AtomicBoolean();

  public MemoryEndPoint(long id, MemoryConnection connection, Selector selector, EndPointServerStatus server, List<String> jmxPath, long pollMicros) throws IOException {
    this(id, connection, selector, server, jmxPath, pollMicros, true);
  }

  MemoryEndPoint(long id, MemoryConnection connection, Selector selector, EndPointServerStatus server, List<String> jmxPath, long pollMicros, boolean activate) throws IOException {
    super(id, server);
    this.connection = connection;
    name = getProtocol() + "_" + connection.remoteAddress() + "_" + id;
    MemoryReadiness registration = null;
    EndPointJMX management = null;
    try {
      if (jmxPath != null && !jmxPath.isEmpty()) {
        management = new EndPointJMX(jmxPath, this);
        jmxParentPath = management.getTypePath();
      } else {
        jmxParentPath = List.of();
      }
      registration = new MemoryReadiness(connection, selector, pollMicros, this::failed);
    } catch (IOException | RuntimeException exception) {
      if (management != null) {
        management.close();
      }
      try {
        connection.close();
      } catch (IOException cleanup) {
        exception.addSuppressed(cleanup);
      }
      super.close();
      throw exception;
    }
    mbean = management;
    readiness = registration;
    if (activate) {
      activate();
    }
  }

  /** Accepted endpoints are published by the listener before peer monitoring can close them. */
  void activate() throws IOException {
    if (connection.isClosed()) {
      close();
    } else {
      readiness.start();
    }
  }

  @Override
  public String getProtocol() {
    return getConfig().getEndPointConfig().getType();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    ensureOpen();
    int count = connection.write(packet.getRawBuffer());
    if (count > 0) {
      readiness.madeProgress();
      updateWriteBytes(count);
    }
    return count;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    ensureOpen();
    int count = connection.read(packet.getRawBuffer());
    if (count < 0) {
      throw new IOException("Memory peer closed");
    }
    if (count > 0) {
      readiness.madeProgress();
      updateReadBytes(count);
    }
    return count;
  }

  @Override
  public boolean hasBufferedReadData() {
    return !closed.get() && connection.hasData();
  }

  @Override
  public FutureTask<SelectionKey> register(int selection, Selectable runner) throws IOException {
    if (closed.get()) {
      throw new ClosedChannelException();
    }
    return readiness.register(selection, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selection) throws ClosedChannelException {
    return readiness.deregister(selection);
  }

  @Override
  public String getAuthenticationConfig() {
    return getConfig().getAuthenticationRealm();
  }

  @Override
  public String getRemoteSocketAddress() {
    return connection.remoteAddress();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(MemoryEndPoint.class.getName() + "_" + getId());
  }

  @Override
  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    IOException failure = null;
    try {
      super.close();
    } catch (IOException | RuntimeException exception) {
      failure = exception instanceof IOException ioException ? ioException : new IOException("Memory endpoint close handler failed", exception);
      // EndPoint invokes its handler before updating counters. Finish that bookkeeping even on handler failure.
      setCloseHandler(null);
      super.close();
    } finally {
      try {
        readiness.close();
      } catch (IOException exception) {
        failure = combine(failure, exception);
      }
      try {
        connection.close();
      } catch (IOException exception) {
        failure = combine(failure, exception);
      } finally {
        if (mbean != null) {
          mbean.close();
        }
        server.handleCloseEndPoint(this);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  private void ensureOpen() throws IOException {
    if (closed.get() || connection.isClosed()) {
      throw new IOException("Memory connection is closed");
    }
  }

  private void failed(Throwable failure) {
    logger.log(ServerLogMessages.SELECTOR_TASK_FAILED_1, failure, name);
    try {
      close();
    } catch (IOException exception) {
      logger.log(ServerLogMessages.END_POINT_CONNECTION_CLOSE_EXCEPTION, exception);
    }
  }

  private static IOException combine(IOException failure, IOException next) {
    if (failure == null) {
      return next;
    }
    failure.addSuppressed(next);
    return failure;
  }
}
