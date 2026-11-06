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

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.memory.MemoryConnection;
import io.mapsmessaging.memory.MemoryServer;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

import java.io.IOException;
import java.util.List;

/** Owns managed acceptance; per-peer protocol I/O is dispatched by the existing selectors. */
public class MemoryEndPointServer extends EndPointServer {
  private final SelectorLoadManager selectors;
  private final List<String> jmxPath;
  private MemoryServer listener;
  private Thread acceptThread;
  private boolean closed;

  public MemoryEndPointServer(AcceptHandler accept, EndPointURL url, EndPointServerConfigDTO config, SelectorLoadManager selectors, EndPointManagerJMX management) {
    super(accept, url, config);
    this.selectors = selectors;
    jmxPath = management == null ? List.of() : management.getTypePath();
  }

  @Override
  public synchronized void start() throws IOException {
    if (listener != null && !listener.isClosed()) {
      throw new IOException("Memory listener is already started");
    }
    MemoryConfigDTO config = (MemoryConfigDTO) getConfig().getEndPointConfig();
    listener = MemoryServer.listen(MemoryEndpointSettings.listener(url, config));
    closed = false;
  }

  @Override
  public synchronized void register() throws IOException {
    if (closed || listener == null || listener.isClosed()) {
      throw new IOException("Memory listener is not started");
    }
    if (acceptThread == null) {
      MemoryServer currentListener = listener;
      acceptThread = Thread.ofVirtual().name("memory-endpoint-accept").unstarted(() -> acceptLoop(currentListener));
      acceptThread.start();
    }
  }

  @Override
  public void deregister() throws IOException {
    Thread stopping;
    synchronized (this) {
      stopping = acceptThread;
      if (stopping != null) {
        stopping.interrupt();
      }
    }
    join(stopping);
    synchronized (this) {
      if (acceptThread == stopping) {
        acceptThread = null;
      }
    }
  }

  private void acceptLoop(MemoryServer currentListener) {
    while (!Thread.currentThread().isInterrupted() && !currentListener.isClosed()) {
      MemoryConnection connection = null;
      MemoryEndPoint endpoint = null;
      try {
        connection = currentListener.accept();
        MemoryConfigDTO config = (MemoryConfigDTO) getConfig().getEndPointConfig();
        endpoint = new MemoryEndPoint(generateID(), connection, selectors.allocate(), this, jmxPath, config.getReadinessPollMicros(), false);
        synchronized (this) {
          if (closed || Thread.currentThread().isInterrupted() || listener != currentListener) {
            endpoint.close();
            return;
          }
          registerNewEndPoint(endpoint);
        }
        endpoint.activate();
        if (!endpoint.isClosed()) {
          acceptHandler.accept(endpoint);
        }
      } catch (IOException | RuntimeException exception) {
        if (!Thread.currentThread().isInterrupted() && !currentListener.isClosed()) {
          incrementError();
          logger.log(ServerLogMessages.END_POINT_MANAGER_ACCEPT_EXCEPTION, exception);
        }
        try {
          if (endpoint != null) {
            endpoint.close();
          } else if (connection != null) {
            connection.close();
          }
        } catch (IOException cleanup) {
          logger.log(ServerLogMessages.END_POINT_CONNECTION_CLOSE_EXCEPTION, cleanup);
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    MemoryServer closingListener;
    Thread stopping;
    synchronized (this) {
      if (closed) {
        return;
      }
      closed = true;
      closingListener = listener;
      stopping = acceptThread;
      if (stopping != null) {
        stopping.interrupt();
      }
    }
    IOException failure = null;
    if (closingListener != null) {
      try {
        closingListener.close();
      } catch (IOException exception) {
        failure = exception;
      }
    }
    for (EndPoint endpoint : getActiveEndPoints()) {
      try {
        endpoint.close();
      } catch (IOException exception) {
        failure = combine(failure, exception);
      }
    }
    try {
      join(stopping);
    } catch (IOException exception) {
      failure = combine(failure, exception);
    }
    synchronized (this) {
      if (acceptThread == stopping) {
        acceptThread = null;
      }
    }
    try {
      super.close();
    } catch (IOException exception) {
      failure = combine(failure, exception);
    }
    if (failure != null) {
      throw failure;
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // Managed acceptance is blocking and runs only on its dedicated virtual thread.
  }

  @Override
  protected Logger createLogger(String name) {
    return LoggerFactory.getLogger(MemoryEndPointServer.class.getName() + "_" + name);
  }

  private static void join(Thread thread) throws IOException {
    if (thread == null || thread == Thread.currentThread()) {
      return;
    }
    try {
      thread.join(5000);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted stopping memory acceptance", exception);
    }
    if (thread.isAlive()) {
      throw new IOException("Memory acceptance did not stop within 5000 ms");
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
