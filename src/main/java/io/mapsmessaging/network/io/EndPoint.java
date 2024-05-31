/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io;

import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import javax.security.auth.Subject;
import lombok.Getter;
import lombok.Setter;

public abstract class EndPoint implements Closeable {

  public static final LongAdder currentConnections = new LongAdder();
  public static final LongAdder totalReadBytes = new LongAdder();
  public static final LongAdder totalWriteBytes = new LongAdder();
  public static final LongAdder totalConnections = new LongAdder();
  public static final LongAdder totalDisconnections = new LongAdder();

  @Getter
  protected final EndPointServerStatus server;
  @Getter
  protected final Logger logger;

  private final AtomicLong lastRead = new AtomicLong();
  private final AtomicLong lastWrite = new AtomicLong();

  private final LinkedMovingAverages readByteAverages;
  private final LinkedMovingAverages writeByteAverages;

  private final LinkedMovingAverages bufferOverFlow;
  private final LinkedMovingAverages bufferUnderFlow;

  private final boolean isClient;
  @Getter
  private final long id;
  @Getter
  private final long connected = System.currentTimeMillis();
  private boolean isClosed;

  protected List<String> jmxParentPath;
  @Setter
  private CloseHandler closeHandler;

  @Getter
  @Setter
  private ProtocolImpl boundProtocol;

  protected EndPoint(long id, EndPointServerStatus server) {
    this.server = server;
    isClient = !(server instanceof EndPointServer);
    this.id = id;

    readByteAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Read Bytes", 1, 5, 4, TimeUnit.MINUTES, "Bytes");
    writeByteAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Write Bytes", 1, 5, 4, TimeUnit.MINUTES, "Bytes");
    bufferOverFlow = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Buffer Overflow", 1, 5, 4, TimeUnit.MINUTES, "Packets");
    bufferUnderFlow = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Buffer Underflow", 1, 5, 4, TimeUnit.MINUTES, "Packets");
    logger = createLogger();
    totalConnections.increment();
    currentConnections.increment();
    isClosed = false;
  }

  @Override
  public void close() throws IOException {
    if (closeHandler != null) {
      closeHandler.close();
    }
    synchronized (this) {
      if (!isClosed) {
        isClosed = true;
        totalDisconnections.increment();
        currentConnections.decrement();
      }
    }
  }

  public boolean isClient() {
    return isClient;
  }

  public void updateReadBytes(int read) {
    readByteAverages.add(read);
    totalReadBytes.add(read);
    lastRead.set(System.currentTimeMillis());
    if (server != null) {
      server.updateBytesRead(read);
      server.incrementPacketsRead();
    }
  }

  public void updateWriteBytes(int wrote) {
    writeByteAverages.add(wrote);
    totalWriteBytes.add(wrote);
    lastWrite.set(System.currentTimeMillis());
    if (server != null) {
      server.updateBytesSent(wrote);
      server.incrementPacketsSent();
    }
  }

  public void completedConnection() {

  }

  public EndPointServerConfig getConfig() {
    return server.getConfig();
  }

  public List<String> getJMXTypePath() {
    return jmxParentPath;
  }

  public boolean isUDP() {
    return false;
  }

  public LinkedMovingAverages getReadBytes() {
    return readByteAverages;
  }

  public LinkedMovingAverages getWriteBytes() {
    return writeByteAverages;
  }

  public long getLastRead() {
    return lastRead.get();
  }

  public long getLastWrite() {
    return lastWrite.get();
  }

  public Subject getEndPointSubject() {
    if(boundProtocol != null){
      return boundProtocol.getSubject();
    }
    return null;
  }

  public Principal getEndPointPrincipal() {
    return null;
  }

  public boolean isSSL() {
    return false;
  }

  public LinkedMovingAverages getOverFlow() {
    return bufferOverFlow;
  }

  public LinkedMovingAverages getUnderFlow() {
    return bufferUnderFlow;
  }

  public long getOverFlowTotal() {
    return bufferOverFlow.getTotal();
  }

  public long getUnderFlowTotal() {
    return bufferUnderFlow.getTotal();
  }

  public void incrementOverFlow() {
    bufferOverFlow.increment();
  }

  public void incrementUnderFlow() {
    bufferUnderFlow.increment();
  }

  public abstract String getProtocol();

  public abstract int sendPacket(Packet packet) throws IOException;

  public abstract int readPacket(Packet packet) throws IOException;

  public abstract FutureTask<SelectionKey> register(int selectionKey, Selectable runner)
      throws IOException;

  public abstract FutureTask<SelectionKey> deregister(int selectionKey)
      throws ClosedChannelException;

  public abstract String getAuthenticationConfig();

  public abstract String getName();

  protected abstract Logger createLogger();

}
