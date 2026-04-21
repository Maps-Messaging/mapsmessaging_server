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
 *
 */

package io.mapsmessaging.state.drone.tak;

import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;


@Getter
public class TakSocketConnection implements Closeable {

  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
  private static final int DEFAULT_SOCKET_TIMEOUT_MS = 5000;
  private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;
  private static final long RECONNECT_DELAY_MS = 1000;

  private final String host;
  private final int port;
  private final int connectTimeoutMs;
  private final int socketTimeoutMs;
  private final boolean appendNewLine;
  private final int maxQueueSize;

  private final LinkedBlockingDeque<String> queue;
  private final Thread writerThread;

  private volatile boolean running;

  private Socket socket;
  private OutputStream socketOutputStream;

  public TakSocketConnection(String host, int port) {
    this(host, port, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_SOCKET_TIMEOUT_MS, true, DEFAULT_MAX_QUEUE_SIZE);
  }

  public TakSocketConnection(String host,
                             int port,
                             int connectTimeoutMs,
                             int socketTimeoutMs,
                             boolean appendNewLine,
                             int maxQueueSize) {
    this.host = Objects.requireNonNull(host, "host cannot be null");
    this.port = port;
    this.connectTimeoutMs = connectTimeoutMs;
    this.socketTimeoutMs = socketTimeoutMs;
    this.appendNewLine = appendNewLine;
    this.maxQueueSize = Math.max(1, maxQueueSize);
    this.queue = new LinkedBlockingDeque<>(this.maxQueueSize);
    this.running = true;
    this.socket = null;
    this.socketOutputStream = null;

    this.writerThread = new Thread(this::writerLoop, "tak-socket-writer-" + host + "-" + port);
    this.writerThread.setDaemon(true);
    this.writerThread.start();
  }

  public void accept(String xml) {
    if (xml == null || xml.isBlank() || !running) {
      return;
    }

    synchronized (queue) {
      if (queue.remainingCapacity() == 0) {
        queue.pollFirst();
      }
      queue.offerLast(xml);
    }
  }

  private void writerLoop() {
    while (running) {
      String xml;
      try {
        xml = queue.takeFirst();
      }
      catch (InterruptedException interruptedException) {
        if (!running) {
          Thread.currentThread().interrupt();
          break;
        }
        continue;
      }

      boolean sent = false;
      while (running && !sent) {
        try {
          ensureConnected();
          write(xml);
          sent = true;
        }
        catch (IOException ioException) {
          closeQuietly();

          if (!running) {
            break;
          }

          try {
            Thread.sleep(RECONNECT_DELAY_MS);
          }
          catch (InterruptedException interruptedException) {
            if (!running) {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }
      }
    }
  }

  private void ensureConnected() throws IOException {
    if (isConnected()) {
      return;
    }
    reconnect();
    if (!isConnected()) {
      throw new IOException("Unable to connect to TAK endpoint " + host + ":" + port);
    }
  }

  private synchronized void write(String xml) throws IOException {
    String t = xml.replace("—", "-");
    socketOutputStream.write(t.getBytes(StandardCharsets.UTF_8));
    socketOutputStream.write('\n');
    socketOutputStream.flush();
  }

  private synchronized void reconnect() {
    closeQuietly();

    try {
      Socket newSocket = new Socket();
      newSocket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
      newSocket.setSoTimeout(socketTimeoutMs);
      newSocket.setKeepAlive(true);
      newSocket.setTcpNoDelay(true);

      socket = newSocket;
      socketOutputStream = newSocket.getOutputStream();
    }
    catch (IOException ignored) {
      socket = null;
      socketOutputStream = null;
    }
  }

  public synchronized boolean isConnected() {
    return socket != null &&
        socket.isConnected() &&
        !socket.isClosed() &&
        socketOutputStream != null;
  }

  @Override
  public void close() {
    running = false;
    writerThread.interrupt();
    closeQuietly();
    queue.clear();
  }

  private synchronized void closeQuietly() {
    if (socketOutputStream != null) {
      try {
        socketOutputStream.close();
      }
      catch (IOException ignored) {
      }
      socketOutputStream = null;
    }

    if (socket != null) {
      try {
        socket.close();
      }
      catch (IOException ignored) {
      }
      socket = null;
    }
  }
}